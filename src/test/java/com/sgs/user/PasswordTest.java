package com.sgs.user;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordTest {

    @Test
    void generatedPasswordsAreStrongAndUnique() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            String p = PasswordGenerator.generate(12);
            assertEquals(12, p.length());
            assertTrue(p.chars().anyMatch(Character::isUpperCase), "needs upper: " + p);
            assertTrue(p.chars().anyMatch(Character::isLowerCase), "needs lower: " + p);
            assertTrue(p.chars().anyMatch(Character::isDigit), "needs digit: " + p);
            assertTrue(p.chars().noneMatch(c -> "0O1lI".indexOf(c) >= 0), "ambiguous char: " + p);
            seen.add(p);
        }
        assertEquals(500, seen.size());
        assertNotNull(PasswordRules.check(PasswordGenerator.generate(12)) == null ? "ok" : null);
        assertThrows(IllegalArgumentException.class, () -> PasswordGenerator.generate(4));
    }

    @Test
    void passwordRules() {
        assertNotNull(PasswordRules.check("short"));
        assertNotNull(PasswordRules.check(null));
        assertNotNull(PasswordRules.check("a".repeat(73)));
        assertNotNull(PasswordRules.check("é".repeat(40)));          // 80 bytes in UTF-8
        assertEquals(null, PasswordRules.check("Valid-Pass-123"));
        assertEquals(null, PasswordRules.check("a".repeat(72)));
    }
}
