package com.sgs.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;

public final class UserDtos {
    private UserDtos() {}

    public record LoginRequest(@NotBlank @Size(max = 50) String username,
                               @NotBlank @Size(max = 128) String password) {}

    public record ChangePasswordRequest(@NotBlank @Size(max = 128) String currentPassword,
                                        @NotBlank @Size(min = 8, max = 72) String newPassword) {}

    public record UserInfo(Long id, String username, String fullName, Role role, boolean mustChangePassword,
                           boolean canUploadImages, Instant uploadPermissionUntil) {
        public static UserInfo from(User u, Instant now) {
            return new UserInfo(u.getId(), u.getUsername(), u.getFullName(), u.getRole(), u.isMustChangePassword(),
                    u.canUploadNow(now),
                    u.getRole() == Role.EMPLOYEE ? u.getUploadPermissionUntil() : null);
        }
    }

    public record CreateEmployeeRequest(
            @NotBlank @Pattern(regexp = "^[a-zA-Z0-9._-]{4,30}$",
                    message = "Username must be 4-30 characters: letters, digits, dot, underscore or hyphen") String username,
            @NotBlank @Size(max = 100) String fullName,
            @Size(max = 30) String phone,
            @Size(min = 8, max = 72) String password,        // optional: generated when omitted
            Boolean mustChangePassword) {}

    public record UpdateEmployeeRequest(@NotBlank @Size(max = 100) String fullName, @Size(max = 30) String phone) {}

    /** type: TEMPORARY (needs days or untilDate) or PERMANENT. */
    public record SuspendRequest(@NotNull SuspensionType type,
                                 @Min(1) @Max(3650) Integer days,
                                 LocalDate untilDate,
                                 @Size(max = 255) String reason) {}

    /** username is mandatory on purpose: it forces the admin to confirm WHICH account is being reset. */
    public record ResetPasswordRequest(@NotBlank @Size(max = 50) String username,
                                       @Size(min = 8, max = 72) String newPassword) {}

    public record ResetPasswordResult(String username, String temporaryPassword) {}

    /** Grant for N hours, or until the end of untilDate. Exactly one of them. */
    public record UploadPermissionRequest(@Min(1) @Max(720) Integer hours, LocalDate untilDate) {}

    public record EmployeeDto(Long id, String username, String fullName, String phone, AccountState state,
                              Instant suspendedUntil, String suspensionReason, boolean uploadPermissionActive,
                              Instant uploadPermissionUntil, boolean mustChangePassword, Instant lastLoginAt,
                              Instant createdAt) {
        public static EmployeeDto from(User u, Instant now) {
            AccountState st = u.state(now);
            boolean uploadActive = u.getUploadPermissionUntil() != null && now.isBefore(u.getUploadPermissionUntil());
            return new EmployeeDto(u.getId(), u.getUsername(), u.getFullName(), u.getPhone(), st,
                    st == AccountState.SUSPENDED_TEMPORARY ? u.getSuspendedUntil() : null,
                    st == AccountState.ACTIVE ? null : u.getSuspensionReason(),
                    uploadActive, uploadActive ? u.getUploadPermissionUntil() : null,
                    u.isMustChangePassword(), u.getLastLoginAt(), u.getCreatedAt());
        }
    }

    public record EmployeeCreated(EmployeeDto employee, String generatedPassword) {}
}
