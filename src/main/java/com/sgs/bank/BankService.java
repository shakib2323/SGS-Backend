package com.sgs.bank;

import com.sgs.audit.AuditService;
import com.sgs.bank.BankDtos.AddBankEntryRequest;
import com.sgs.bank.BankDtos.AdminBankEntryDto;
import com.sgs.bank.BankDtos.AdminBankOverviewDto;
import com.sgs.bank.BankDtos.AdminEmployeeBankSummaryDto;
import com.sgs.bank.BankDtos.BankAuditDto;
import com.sgs.bank.BankDtos.BankEntryDto;
import com.sgs.bank.BankDtos.BankSummaryDto;
import com.sgs.bank.BankDtos.DayTotalDto;
import com.sgs.bank.BankDtos.EditBankEntryRequest;
import com.sgs.bank.BankDtos.EmployeeTotalDto;
import com.sgs.common.ApiException;
import com.sgs.common.PageDto;
import com.sgs.common.TextUtil;
import com.sgs.config.AppProperties;
import com.sgs.security.AuthUser;
import com.sgs.user.Role;
import com.sgs.user.User;
import com.sgs.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BankService {

    private static final LocalDate EPOCH = LocalDate.of(2000, 1, 1);

    private final BankEntryRepository entries;
    private final BankEntryAuditRepository history;
    private final UserRepository users;
    private final AuditService audit;
    private final AppProperties props;
    private final Clock clock;

    private ZoneId zone() {
        return ZoneId.of(props.timezone());
    }

    private LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), zone());
    }

    // =====================================================================
    // Employee side: add + read own. There is intentionally no update/delete here.
    // =====================================================================

    @Transactional
    public BankEntryDto add(AuthUser actor, AddBankEntryRequest r) {
        User u = users.findById(actor.id()).orElseThrow(() -> ApiException.forbidden("FORBIDDEN", "Account not found"));
        if (u.getRole() != Role.EMPLOYEE) throw ApiException.forbidden("FORBIDDEN", "Only employees can add bank entries");

        BigDecimal amount = normalize(r.amount());
        Instant now = clock.instant();
        // Guards against an accidental double-click / double-submit creating the same entry twice.
        if (entries.existsByEmployeeIdAndAmountAndDeletedFalseAndCreatedAtAfter(u.getId(), amount, now.minusSeconds(20))) {
            throw ApiException.conflict("An identical entry was just added. Wait a moment before adding it again.");
        }

        BankEntry e = new BankEntry();
        e.setEmployee(u);
        e.setAmount(amount);
        e.setNote(TextUtil.blankToNull(r.note()));
        e.setEntryDate(LocalDate.ofInstant(now, zone()));
        entries.save(e);
        return toDto(e);
    }

    @Transactional(readOnly = true)
    public PageDto<BankEntryDto> listOwn(AuthUser actor, LocalDate from, LocalDate to, int page, int size) {
        Pageable pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<BankEntry> p = entries.findByEmployeeIdAndDeletedFalseAndEntryDateBetween(
                actor.id(), from == null ? EPOCH : from, to == null ? today() : to, pr);
        return PageDto.of(p.map(BankService::toDto));
    }

    @Transactional(readOnly = true)
    public BankSummaryDto summaryOwn(AuthUser actor, LocalDate from, LocalDate to) {
        return summary(actor.id(), from, to);
    }

    // =====================================================================
    // Admin side
    // =====================================================================

    @Transactional(readOnly = true)
    public PageDto<AdminBankEntryDto> adminList(Long employeeId, LocalDate from, LocalDate to, boolean includeDeleted,
                                                int page, int size) {
        Pageable pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<BankEntry> p = entries.findAll(adminSpec(employeeId, from, to, includeDeleted), pr);
        return PageDto.of(p.map(BankService::toAdminDto));
    }

    @Transactional(readOnly = true)
    public AdminBankOverviewDto adminOverview(LocalDate from, LocalDate to) {
        LocalDate f = from == null ? EPOCH : from;
        LocalDate t = to == null ? today() : to;
        List<EmployeeTotalDto> perEmployee = entries.employeeTotals(f, t).stream()
                .map(v -> new EmployeeTotalDto(v.getEmployeeId(), v.getUsername(), v.getFullName(), v.getTotalAmount(), v.getEntryCount()))
                .toList();
        BigDecimal grand = perEmployee.stream().map(EmployeeTotalDto::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<DayTotalDto> days = entries.dailyTotalsAll(f, t).stream()
                .map(v -> new DayTotalDto(v.getEntryDay(), v.getTotalAmount(), v.getEntryCount())).toList();
        return new AdminBankOverviewDto(f, t, grand, perEmployee, days);
    }

    /** Works for ANY employee, including permanently suspended ones. */
    @Transactional(readOnly = true)
    public AdminEmployeeBankSummaryDto adminEmployeeSummary(Long employeeId, LocalDate from, LocalDate to) {
        User u = users.findById(employeeId).filter(x -> x.getRole() == Role.EMPLOYEE)
                .orElseThrow(() -> ApiException.notFound("Employee"));
        return new AdminEmployeeBankSummaryDto(u.getId(), u.getUsername(), u.getFullName(), u.state(clock.instant()),
                summary(employeeId, from, to));
    }

    @Transactional
    public AdminBankEntryDto adminEdit(Long id, EditBankEntryRequest r, AuthUser actor) {
        BankEntry e = find(id);
        if (e.isDeleted()) throw ApiException.conflict("This entry is deleted. Restore it before editing.");
        BigDecimal newAmount = normalize(r.amount());
        String newNote = TextUtil.blankToNull(r.note());
        String reason = TextUtil.clean(r.reason());

        BankEntryAudit h = record(e, "EDIT", actor, reason);
        h.setNewAmount(newAmount);
        h.setNewNote(newNote);
        history.save(h);

        BigDecimal old = e.getAmount();
        e.setAmount(newAmount);
        e.setNote(newNote);
        audit.log(actor, "BANK_EDIT", "BankEntry", id, "amount " + old + " -> " + newAmount + "; reason: " + reason);
        return toAdminDto(e);
    }

    /** Soft delete: the row stays for accounting/audit, but disappears from totals and from the employee. */
    @Transactional
    public AdminBankEntryDto adminDelete(Long id, String reason, AuthUser actor) {
        BankEntry e = find(id);
        if (e.isDeleted()) throw ApiException.conflict("Entry is already deleted");
        String why = requireReason(reason);
        history.save(record(e, "DELETE", actor, why));
        e.setDeleted(true);
        e.setDeletedAt(clock.instant());
        e.setDeletedBy(actor.id());
        e.setDeleteReason(why);
        audit.log(actor, "BANK_DELETE", "BankEntry", id, "amount " + e.getAmount() + "; reason: " + why);
        return toAdminDto(e);
    }

    @Transactional
    public AdminBankEntryDto adminRestore(Long id, String reason, AuthUser actor) {
        BankEntry e = find(id);
        if (!e.isDeleted()) throw ApiException.conflict("Entry is not deleted");
        String why = requireReason(reason);
        history.save(record(e, "RESTORE", actor, why));
        e.setDeleted(false);
        e.setDeletedAt(null);
        e.setDeletedBy(null);
        e.setDeleteReason(null);
        audit.log(actor, "BANK_RESTORE", "BankEntry", id, "amount " + e.getAmount() + "; reason: " + why);
        return toAdminDto(e);
    }

    @Transactional(readOnly = true)
    public List<BankAuditDto> adminHistory(Long id) {
        find(id);
        return history.findByEntryIdOrderByIdDesc(id).stream()
                .map(h -> new BankAuditDto(h.getId(), h.getAction(), h.getOldAmount(), h.getNewAmount(), h.getOldNote(),
                        h.getNewNote(), h.getReason(), h.getActedByUsername(), h.getActedAt()))
                .toList();
    }

    // =====================================================================
    // helpers
    // =====================================================================

    private BankSummaryDto summary(Long employeeId, LocalDate from, LocalDate to) {
        LocalDate f = from == null ? EPOCH : from;
        LocalDate t = to == null ? today() : to;
        BigDecimal grand = orZero(entries.sumForEmployee(employeeId));
        BigDecimal range = orZero(entries.sumForEmployeeBetween(employeeId, f, t));
        long count = entries.countByEmployeeIdAndDeletedFalseAndEntryDateBetween(employeeId, f, t);
        List<DayTotalDto> days = entries.dailyTotalsForEmployee(employeeId, f, t).stream()
                .map(v -> new DayTotalDto(v.getEntryDay(), v.getTotalAmount(), v.getEntryCount())).toList();
        return new BankSummaryDto(f, t, grand, range, count, days);
    }

    private BankEntry find(Long id) {
        return entries.findById(id).orElseThrow(() -> ApiException.notFound("Bank entry"));
    }

    private BankEntryAudit record(BankEntry e, String action, AuthUser actor, String reason) {
        BankEntryAudit h = new BankEntryAudit();
        h.setEntryId(e.getId());
        h.setAction(action);
        h.setOldAmount(e.getAmount());
        h.setOldNote(e.getNote());
        h.setReason(reason);
        h.setActedBy(actor.id());
        h.setActedByUsername(actor.username());
        h.setActedAt(clock.instant());
        return h;
    }

    private BigDecimal normalize(BigDecimal amount) {
        BigDecimal a;
        try {
            a = amount.setScale(2, java.math.RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw ApiException.badRequest("Amount can have at most 2 decimal places");
        }
        if (a.signum() <= 0) throw ApiException.badRequest("Amount must be greater than zero");
        if (a.compareTo(props.limits().maxBankAmount()) > 0) {
            throw ApiException.badRequest("Amount exceeds the allowed maximum of " + props.limits().maxBankAmount());
        }
        return a;
    }

    private static String requireReason(String reason) {
        String r = TextUtil.blankToNull(reason);
        if (r == null) throw ApiException.badRequest("A reason is required");
        if (r.length() > 255) throw ApiException.badRequest("Reason is too long (max 255 characters)");
        return r;
    }

    private static BigDecimal orZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static Specification<BankEntry> adminSpec(Long employeeId, LocalDate from, LocalDate to, boolean includeDeleted) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (employeeId != null) ps.add(cb.equal(root.get("employee").get("id"), employeeId));
            if (from != null) ps.add(cb.greaterThanOrEqualTo(root.get("entryDate"), from));
            if (to != null) ps.add(cb.lessThanOrEqualTo(root.get("entryDate"), to));
            if (!includeDeleted) ps.add(cb.isFalse(root.get("deleted")));
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    private static BankEntryDto toDto(BankEntry e) {
        return new BankEntryDto(e.getId(), e.getAmount(), e.getNote(), e.getEntryDate(), e.getCreatedAt());
    }

    private static AdminBankEntryDto toAdminDto(BankEntry e) {
        User u = e.getEmployee();
        return new AdminBankEntryDto(e.getId(), u.getId(), u.getUsername(), u.getFullName(), e.getAmount(), e.getNote(),
                e.getEntryDate(), e.getCreatedAt(), e.getUpdatedAt(), e.isDeleted(), e.getDeletedAt(), e.getDeleteReason());
    }
}
