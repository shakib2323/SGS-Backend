package com.sgs.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgs.config.AppProperties;
import com.sgs.user.User;
import com.sgs.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Cookie-JWT authentication. Every request re-reads the user, so suspension, role changes, password resets
 * and token-version bumps take effect immediately (no waiting for the token to expire).
 *
 * CSRF: the token lives in an HttpOnly SameSite cookie, and state-changing requests must also carry
 * "X-Requested-With: XMLHttpRequest". A custom header forces a CORS preflight, which only our allow-listed
 * origin passes, so a third-party page cannot forge writes.
 *
 * Not a Spring bean on purpose (a Filter bean would be registered twice).
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String ATTR_AUTH_ERROR = "sgs.authError";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final JwtService jwt;
    private final UserRepository users;
    private final AppProperties props;
    private final ObjectMapper mapper;
    private final Clock clock;

    public JwtAuthFilter(JwtService jwt, UserRepository users, AppProperties props, ObjectMapper mapper, Clock clock) {
        this.jwt = jwt;
        this.users = users;
        this.props = props;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String token = readCookie(req);
        // if (token != null) {
        //     Optional<JwtService.Parsed> parsed = jwt.parse(token);
        //     if (parsed.isPresent()) {
        //         User u = users.findById(parsed.get().userId()).orElse(null);
        //         Instant now = clock.instant();
        //         if (u != null && u.getTokenVersion() == parsed.get().tokenVersion()) {
        //             if (u.isSuspended(now)) {
        //                 req.setAttribute(ATTR_AUTH_ERROR, "ACCOUNT_SUSPENDED");
        //             } else {
        //                 String path = req.getRequestURI();
        //                 boolean protectedPath = isProtected(path);
        //                 if (protectedPath && !SAFE_METHODS.contains(req.getMethod())
        //                         && !"XMLHttpRequest".equals(req.getHeader("X-Requested-With"))) {
        //                     JsonErrors.write(mapper, res, 403, "CSRF_HEADER_MISSING",
        //                             "Missing X-Requested-With header");
        //                     return;
        //                 }
        //                 if (protectedPath && u.isMustChangePassword() && !path.startsWith("/api/auth/")) {
        //                     JsonErrors.write(mapper, res, 403, "PASSWORD_CHANGE_REQUIRED",
        //                             "You must change your password before continuing");
        //                     return;
        //                 }
        //                 AuthUser principal = new AuthUser(u.getId(), u.getUsername(), u.getRole());
        //                 var auth = new UsernamePasswordAuthenticationToken(principal, null,
        //                         List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
        //                 SecurityContextHolder.getContext().setAuthentication(auth);
        //             }
        //         }
        //     }
        // }
        // chain.doFilter(req, res);


        //         String token = readCookie(req);

if (token != null) {
    Optional<JwtService.Parsed> parsed = jwt.parse(token);

    if (parsed.isPresent()) {
        JwtService.Parsed claims = parsed.get();
        User user = users.findById(claims.userId()).orElse(null);

        if (user != null && user.getTokenVersion() == claims.tokenVersion()) {
            Instant now = clock.instant();

            if (user.isSuspended(now)) {
                req.setAttribute(ATTR_AUTH_ERROR, "ACCOUNT_SUSPENDED");
            } else {
                String path = req.getRequestURI();
                boolean protectedPath = isProtected(path);

                if (protectedPath
                        && !SAFE_METHODS.contains(req.getMethod())
                        && !"XMLHttpRequest".equals(req.getHeader("X-Requested-With"))) {
                    JsonErrors.write(mapper, res, 403, "CSRF_HEADER_MISSING",
                            "Missing X-Requested-With header");
                    return;
                }

                if (protectedPath && user.isMustChangePassword()
                        && !path.startsWith("/api/auth/")) {
                    JsonErrors.write(mapper, res, 403, "PASSWORD_CHANGE_REQUIRED",
                            "You must change your password before continuing");
                    return;
                }

                AuthUser principal =
                        new AuthUser(user.getId(), user.getUsername(), user.getRole());

                var auth = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                );

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
    }
}

chain.doFilter(req, res);
    }

    private static boolean isProtected(String path) {
        return path.startsWith("/api/") && !path.startsWith("/api/public/")
                && !path.equals("/api/auth/login") && !path.equals("/api/auth/logout");
    }

    private String readCookie(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (props.cookie().name().equals(c.getName())) return c.getValue();
        }
        return null;
    }
}
