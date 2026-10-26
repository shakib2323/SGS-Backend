package com.sgs.common;

/** Small, dependency-free text helpers. */
public final class TextUtil {
    private TextUtil() {}

    /** Strips control characters (keeps \n, \r, \t), then trims. */
    public static String clean(String s) {
        if (s == null) return null;
        return s.replaceAll("[\\p{Cntrl}&&[^\\n\\r\\t]]", "").strip();
    }

    public static String blankToNull(String s) {
        String t = clean(s);
        return (t == null || t.isEmpty()) ? null : t;
    }

    /** Escapes LIKE wildcards so user input is matched literally (use with escape char '\\'). */
    public static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
