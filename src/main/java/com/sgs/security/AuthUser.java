package com.sgs.security;

import com.sgs.user.Role;

/** The authenticated principal placed in the SecurityContext (inject with @AuthenticationPrincipal). */
public record AuthUser(Long id, String username, Role role) {
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
