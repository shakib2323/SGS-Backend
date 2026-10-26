package com.sgs.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

public final class Slugs {
    private Slugs() {}

    public static String slugify(String input) {
        if (input == null) return "item";
        String s = Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        s = s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (s.length() > 80) s = s.substring(0, 80).replaceAll("-+$", "");
        return s.isEmpty() ? "item" : s;
    }

    /** Returns base, base-2, base-3 ... until {@code taken} says the candidate is free. */
    public static String unique(String base, Predicate<String> taken) {
        String candidate = base;
        int n = 2;
        while (taken.test(candidate)) {
            candidate = base + "-" + n++;
        }
        return candidate;
    }
}
