package com.sgs.common;

import java.net.URI;

/** Pure URL validation used by {@link SafeUrlValidator}. Only absolute http(s) URLs are accepted. */
public final class UrlRules {
    private UrlRules() {}

    public static boolean isSafeHttpUrl(String value, int maxLength) {
        if (value == null || value.isBlank()) return true;           // optional field
        String v = value.trim();
        if (v.length() > maxLength) return false;
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (Character.isWhitespace(c) || Character.isISOControl(c)) return false;
        }
        try {
            URI uri = new URI(v);
            String scheme = uri.getScheme();
            if (scheme == null) return false;
            if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) return false;
            String host = uri.getHost();
            return host != null && !host.isBlank();
        } catch (Exception e) {
            return false;
        }
    }
}
