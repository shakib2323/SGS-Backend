package com.sgs.bank;

import com.sgs.bank.BankDtos.AdminBankEntryDto;
import com.sgs.bank.BankDtos.AdminBankOverviewDto;
import com.sgs.bank.BankDtos.AdminEmployeeBankSummaryDto;
import com.sgs.bank.BankDtos.BankAuditDto;
import com.sgs.bank.BankDtos.EditBankEntryRequest;
import com.sgs.common.PageDto;
import com.sgs.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/bank")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminBankController {

    private final BankService service;

    @GetMapping
    public PageDto<AdminBankEntryDto> list(@RequestParam(name = "employeeId", required = false) Long employeeId,
                                           @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                           @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                           @RequestParam(name = "includeDeleted", defaultValue = "false") boolean includeDeleted,
                                           @RequestParam(name = "page", defaultValue = "0") int page,
                                           @RequestParam(name = "size", defaultValue = "50") int size) {
        return service.adminList(employeeId, from, to, includeDeleted, page, size);
    }

    /** Per-employee totals + date-wise totals across everyone. */
    @GetMapping("/overview")
    public AdminBankOverviewDto overview(@RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.adminOverview(from, to);
    }

    @GetMapping("/employees/{employeeId}/summary")
    public AdminEmployeeBankSummaryDto employeeSummary(@PathVariable("employeeId") Long employeeId,
                                                       @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                       @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.adminEmployeeSummary(employeeId, from, to);
    }

    @PutMapping("/{id}")
    public AdminBankEntryDto edit(@PathVariable("id") Long id, @Valid @RequestBody EditBankEntryRequest r,
                                  @AuthenticationPrincipal AuthUser actor) {
        return service.adminEdit(id, r, actor);
    }

    @DeleteMapping("/{id}")
    public AdminBankEntryDto delete(@PathVariable("id") Long id, @RequestParam("reason") String reason,
                                    @AuthenticationPrincipal AuthUser actor) {
        return service.adminDelete(id, reason, actor);
    }

    @PostMapping("/{id}/restore")
    public AdminBankEntryDto restore(@PathVariable("id") Long id, @RequestParam("reason") String reason,
                                     @AuthenticationPrincipal AuthUser actor) {
        return service.adminRestore(id, reason, actor);
    }

    @GetMapping("/{id}/history")
    public List<BankAuditDto> history(@PathVariable("id") Long id) {
        return service.adminHistory(id);
    }
}
