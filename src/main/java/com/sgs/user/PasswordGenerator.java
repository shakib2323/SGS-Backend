package com.sgs.user;

import java.security.SecureRandom;

public final class PasswordGenerator {
    // No 0/O/1/l/I so a password read out over the phone is not misheard.
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGIT = "23456789";
    private static final String ALL = UPPER + LOWER + DIGIT;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordGenerator() {}

    public static String generate(int length) {
        if (length < 8) throw new IllegalArgumentException("length must be >= 8");
        char[] c = new char[length];
        c[0] = pick(UPPER);
        c[1] = pick(LOWER);
        c[2] = pick(DIGIT);
        for (int i = 3; i < length; i++) c[i] = pick(ALL);
        for (int i = length - 1; i > 0; i--) {            // Fisher-Yates shuffle
            int j = RANDOM.nextInt(i + 1);
            char t = c[i];
            c[i] = c[j];
            c[j] = t;
        }
        return new String(c);
    }

    private static char pick(String s) {
        return s.charAt(RANDOM.nextInt(s.length()));
    }
}
