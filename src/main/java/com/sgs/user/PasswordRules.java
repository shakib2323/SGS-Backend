package com.sgs.user;

import java.nio.charset.StandardCharsets;

/** BCrypt silently/loudly breaks beyond 72 bytes, so we enforce the limit up front. */
public final class PasswordRules {
    private PasswordRules() {}

    /** @return an error message, or null when the password is acceptable. */
    public static String check(String password) {
        if (password == null || password.length() < 8) return "Password must be at least 8 characters";
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) return "Password is too long (max 72 bytes)";
        if (password.isBlank()) return "Password must not be blank";
        return null;
    }
}
