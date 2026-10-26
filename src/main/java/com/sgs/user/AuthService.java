package com.sgs.user;

import com.sgs.common.ApiException;
import com.sgs.common.RateLimiter;
import com.sgs.config.AppProperties;
import com.sgs.security.AuthUser;
import com.sgs.security.JwtService;
import com.sgs.user.UserDtos.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    public record LoginResult(UserInfo user, String token) {}

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final RateLimiter limiter;
    private final AppProperties props;
    private final Clock clock;

    private volatile String dummyHash;

    @Transactional
    public LoginResult login(String rawUsername, String password, String ip) {
        String username = rawUsername.trim().toLowerCase(Locale.ROOT);
        Duration window = props.limits().loginWindow();
        int max = props.limits().loginMaxFailures();
        String userKey = "login:" + ip + ":" + username;
        String ipKey = "login-ip:" + ip;
        if (limiter.count(userKey, window) >= max || limiter.count(ipKey, window) >= max * 6) {
            throw ApiException.tooMany("Too many failed login attempts. Try again later.");
        }

        User u = users.findByUsername(username).orElse(null);
        boolean tooLong = password.getBytes(StandardCharsets.UTF_8).length > 72;
        boolean ok;
        if (u == null || tooLong) {
            encoder.matches("x", dummyHash());                 // keep timing similar for unknown users
            ok = false;
        } else {
            ok = encoder.matches(password, u.getPasswordHash());
        }
        if (!ok) {
            limiter.hit(userKey, window);
            limiter.hit(ipKey, window);
            throw ApiException.unauthorized("INVALID_CREDENTIALS", "Invalid username or password");
        }

        Instant now = clock.instant();
        if (u.isSuspended(now)) {
            throw ApiException.forbidden("ACCOUNT_SUSPENDED", suspensionMessage(u));
        }
        limiter.clear(userKey);
        u.setLastLoginAt(now);
        return new LoginResult(UserInfo.from(u, now), jwt.issue(u));
    }

    @Transactional(readOnly = true)
    public UserInfo me(AuthUser actor) {
        User u = users.findById(actor.id()).orElseThrow(() -> ApiException.unauthorized("UNAUTHORIZED", "Authentication required"));
        return UserInfo.from(u, clock.instant());
    }

    /** Bumps the token version, so every other session of this user is signed out; returns a fresh token. */
    @Transactional
    public LoginResult changePassword(AuthUser actor, String current, String next) {
        User u = users.findById(actor.id()).orElseThrow(() -> ApiException.unauthorized("UNAUTHORIZED", "Authentication required"));
        if (!encoder.matches(current, u.getPasswordHash())) {
            throw ApiException.badRequest("Current password is incorrect");
        }
        String problem = PasswordRules.check(next);
        if (problem != null) throw ApiException.badRequest(problem);
        if (encoder.matches(next, u.getPasswordHash())) {
            throw ApiException.badRequest("New password must be different from the current one");
        }
        u.setPasswordHash(encoder.encode(next));
        u.setMustChangePassword(false);
        u.setTokenVersion(u.getTokenVersion() + 1);
        return new LoginResult(UserInfo.from(u, clock.instant()), jwt.issue(u));
    }

    private String dummyHash() {
        String h = dummyHash;
        if (h == null) {
            h = encoder.encode("not-a-real-password");
            dummyHash = h;
        }
        return h;
    }

    private String suspensionMessage(User u) {
        if (u.getSuspensionType() == SuspensionType.TEMPORARY && u.getSuspendedUntil() != null) {
            String when = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
                    .withZone(ZoneId.of(props.timezone())).format(u.getSuspendedUntil());
            return "Your account is suspended until " + when + ".";
        }
        return "Your account has been suspended. Please contact the administrator.";
    }
}
