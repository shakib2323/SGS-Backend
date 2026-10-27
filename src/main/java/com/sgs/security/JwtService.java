package com.sgs.security;

import com.sgs.config.AppProperties;
import com.sgs.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    public record Parsed(long userId, int tokenVersion) {}

    private final SecretKey key;
    private final Duration expiry;
    private final Clock clock;

    public JwtService(AppProperties props, Clock clock) {
        // this.key = Keys.hmacShaKeyFor(props.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.key = Keys.hmacShaKeyFor(
    props.jwt().secret().getBytes(StandardCharsets.UTF_8)
);
        this.expiry = props.jwt().expiry() == null ? Duration.ofHours(8) : props.jwt().expiry();
        this.clock = clock;
    }

    public Duration expiry() {
        return expiry;
    }

    /** The token carries only the user id and a version; role/suspension are always read fresh from the DB. */
    public String issue(User user) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("tv", user.getTokenVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiry)))
                .signWith(key)
                .compact();
    }

    public Optional<Parsed> parse(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            Integer tv = c.get("tv", Integer.class);
            return Optional.of(new Parsed(Long.parseLong(c.getSubject()), tv == null ? -1 : tv));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
