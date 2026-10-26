package com.sgs.user;

import com.sgs.audit.AuditService;
import com.sgs.common.ApiException;
import com.sgs.common.TextUtil;
import com.sgs.config.AppProperties;
import com.sgs.security.AuthUser;
import com.sgs.user.UserDtos.CreateEmployeeRequest;
import com.sgs.user.UserDtos.EmployeeCreated;
import com.sgs.user.UserDtos.EmployeeDto;
import com.sgs.user.UserDtos.ResetPasswordRequest;
import com.sgs.user.UserDtos.ResetPasswordResult;
import com.sgs.user.UserDtos.SuspendRequest;
import com.sgs.user.UserDtos.UpdateEmployeeRequest;
import com.sgs.user.UserDtos.UploadPermissionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

/** Admin-only employee account management. Employees can never be created, suspended or reset by anyone else. */
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    private final AppProperties props;
    private final Clock clock;

    private ZoneId zone() {
        return ZoneId.of(props.timezone());
    }

    @Transactional
    public EmployeeCreated create(CreateEmployeeRequest r, AuthUser actor) {
        String username = r.username().trim().toLowerCase(Locale.ROOT);
        if (users.existsByUsername(username)) throw ApiException.conflict("Username already exists");

        String password = r.password();
        boolean generated = password == null || password.isBlank();
        if (generated) {
            password = PasswordGenerator.generate(12);
        } else {
            String problem = PasswordRules.check(password);
            if (problem != null) throw ApiException.badRequest(problem);
        }

        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(password));
        u.setFullName(TextUtil.clean(r.fullName()));
        u.setPhone(TextUtil.blankToNull(r.phone()));
        u.setRole(Role.EMPLOYEE);
        u.setMustChangePassword(r.mustChangePassword() == null || r.mustChangePassword());
        users.save(u);
        audit.log(actor, "EMPLOYEE_CREATE", "User", u.getId(), "username=" + username);
        return new EmployeeCreated(EmployeeDto.from(u, clock.instant()), generated ? password : null);
    }

    @Transactional(readOnly = true)
    public List<EmployeeDto> list(String q, AccountState state) {
        Instant now = clock.instant();
        String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        return users.findByRoleOrderByCreatedAtDesc(Role.EMPLOYEE).stream()
                .filter(u -> needle.isEmpty()
                        || u.getUsername().contains(needle)
                        || u.getFullName().toLowerCase(Locale.ROOT).contains(needle))
                .filter(u -> state == null || u.state(now) == state)
                .map(u -> EmployeeDto.from(u, now))
                .toList();
    }

    @Transactional(readOnly = true)
    public EmployeeDto get(Long id) {
        return EmployeeDto.from(employee(id), clock.instant());
    }

    @Transactional
    public EmployeeDto update(Long id, UpdateEmployeeRequest r, AuthUser actor) {
        User u = employee(id);
        u.setFullName(TextUtil.clean(r.fullName()));
        u.setPhone(TextUtil.blankToNull(r.phone()));
        audit.log(actor, "EMPLOYEE_UPDATE", "User", id, "username=" + u.getUsername());
        return EmployeeDto.from(u, clock.instant());
    }

    @Transactional
    public EmployeeDto suspend(Long id, SuspendRequest r, AuthUser actor) {
        User u = employee(id);
        Instant now = clock.instant();
        String detail;
        if (r.type() == SuspensionType.PERMANENT) {
            u.setSuspensionType(SuspensionType.PERMANENT);
            u.setSuspendedUntil(null);
            detail = "PERMANENT";
        } else if (r.type() == SuspensionType.TEMPORARY) {
            if ((r.days() == null) == (r.untilDate() == null)) {
                throw ApiException.badRequest("For a temporary suspension provide exactly one of: days, untilDate");
            }
            Instant until = r.days() != null
                    ? now.plus(Duration.ofDays(r.days()))
                    : r.untilDate().plusDays(1).atStartOfDay(zone()).toInstant();   // through the end of that day
            if (!until.isAfter(now)) throw ApiException.badRequest("Suspension end must be in the future");
            u.setSuspensionType(SuspensionType.TEMPORARY);
            u.setSuspendedUntil(until);
            detail = "TEMPORARY until " + until;
        } else {
            throw ApiException.badRequest("type must be TEMPORARY or PERMANENT (use unsuspend to lift)");
        }
        u.setSuspensionReason(TextUtil.blankToNull(r.reason()));
        u.setTokenVersion(u.getTokenVersion() + 1);                     // signs the employee out immediately
        audit.log(actor, "EMPLOYEE_SUSPEND", "User", id, u.getUsername() + " " + detail
                + (u.getSuspensionReason() == null ? "" : "; reason: " + u.getSuspensionReason()));
        return EmployeeDto.from(u, now);
    }

    @Transactional
    public EmployeeDto unsuspend(Long id, AuthUser actor) {
        User u = employee(id);
        u.setSuspensionType(SuspensionType.NONE);
        u.setSuspendedUntil(null);
        u.setSuspensionReason(null);
        audit.log(actor, "EMPLOYEE_UNSUSPEND", "User", id, "username=" + u.getUsername());
        return EmployeeDto.from(u, clock.instant());
    }

    @Transactional
    public ResetPasswordResult resetPassword(ResetPasswordRequest r, AuthUser actor) {
        String username = r.username().trim().toLowerCase(Locale.ROOT);
        User u = users.findByUsername(username)
                .filter(x -> x.getRole() == Role.EMPLOYEE)
                .orElseThrow(() -> ApiException.notFound("Employee"));
        String password = r.newPassword();
        boolean generated = password == null || password.isBlank();
        if (generated) {
            password = PasswordGenerator.generate(12);
        } else {
            String problem = PasswordRules.check(password);
            if (problem != null) throw ApiException.badRequest(problem);
        }
        u.setPasswordHash(encoder.encode(password));
        u.setMustChangePassword(true);
        u.setTokenVersion(u.getTokenVersion() + 1);
        audit.log(actor, "EMPLOYEE_PASSWORD_RESET", "User", u.getId(), "username=" + username);
        return new ResetPasswordResult(username, generated ? password : null);
    }

    @Transactional
    public EmployeeDto grantUploadPermission(Long id, UploadPermissionRequest r, AuthUser actor) {
        if ((r.hours() == null) == (r.untilDate() == null)) {
            throw ApiException.badRequest("Provide exactly one of: hours, untilDate");
        }
        User u = employee(id);
        Instant now = clock.instant();
        Instant until = r.hours() != null
                ? now.plus(Duration.ofHours(r.hours()))
                : r.untilDate().plusDays(1).atStartOfDay(zone()).toInstant();
        if (!until.isAfter(now)) throw ApiException.badRequest("Permission end must be in the future");
        u.setUploadPermissionUntil(until);
        audit.log(actor, "UPLOAD_PERMISSION_GRANT", "User", id, u.getUsername() + " until " + until);
        return EmployeeDto.from(u, now);
    }

    @Transactional
    public EmployeeDto revokeUploadPermission(Long id, AuthUser actor) {
        User u = employee(id);
        u.setUploadPermissionUntil(null);
        audit.log(actor, "UPLOAD_PERMISSION_REVOKE", "User", id, "username=" + u.getUsername());
        return EmployeeDto.from(u, clock.instant());
    }

    private User employee(Long id) {
        return users.findById(id)
                .filter(u -> u.getRole() == Role.EMPLOYEE)
                .orElseThrow(() -> ApiException.notFound("Employee"));
    }
}
