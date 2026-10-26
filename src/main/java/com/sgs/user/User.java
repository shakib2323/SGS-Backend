package com.sgs.user;

import com.sgs.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "suspension_type", nullable = false, length = 20)
    private SuspensionType suspensionType = SuspensionType.NONE;

    @Column(name = "suspended_until")
    private Instant suspendedUntil;

    @Column(name = "suspension_reason", length = 255)
    private String suspensionReason;

    @Column(name = "upload_permission_until")
    private Instant uploadPermissionUntil;

    @Column(name = "token_version", nullable = false)
    private int tokenVersion;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /** Temporary suspensions lift themselves: once suspendedUntil has passed the account is ACTIVE again. */
    public AccountState state(Instant now) {
        if (suspensionType == SuspensionType.PERMANENT) return AccountState.SUSPENDED_PERMANENT;
        if (suspensionType == SuspensionType.TEMPORARY && suspendedUntil != null && now.isBefore(suspendedUntil)) {
            return AccountState.SUSPENDED_TEMPORARY;
        }
        return AccountState.ACTIVE;
    }

    public boolean isSuspended(Instant now) {
        return state(now) != AccountState.ACTIVE;
    }

    public boolean canUploadNow(Instant now) {
        return role == Role.ADMIN || (uploadPermissionUntil != null && now.isBefore(uploadPermissionUntil));
    }
}
