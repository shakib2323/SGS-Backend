package com.sgs.common;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIp {
    private ClientIp() {}

    /**
     * Resolves the caller IP. When {@code trustProxyHeaders} is true we trust Cloudflare/Caddy headers,
     * which is only safe if the origin accepts traffic from Cloudflare IP ranges only (see README).
     */
    public static String of(HttpServletRequest request, boolean trustProxyHeaders) {
        if (trustProxyHeaders) {
            String cf = request.getHeader("CF-Connecting-IP");
            if (cf != null && !cf.isBlank()) return truncate(cf.trim());
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) return truncate(xff.split(",")[0].trim());
        }
        return truncate(request.getRemoteAddr());
    }

    private static String truncate(String ip) {
        return ip.length() > 45 ? ip.substring(0, 45) : ip;
    }
}
