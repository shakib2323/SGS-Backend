package com.sgs.user;

import com.sgs.common.ClientIp;
import com.sgs.config.AppProperties;
import com.sgs.security.AuthUser;
import com.sgs.security.JwtService;
import com.sgs.user.UserDtos.ChangePasswordRequest;
import com.sgs.user.UserDtos.LoginRequest;
import com.sgs.user.UserDtos.UserInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;
    private final JwtService jwt;
    private final AppProperties props;

    @PostMapping("/login")
    public ResponseEntity<UserInfo> login(@Valid @RequestBody LoginRequest r, HttpServletRequest req) {
        AuthService.LoginResult res = auth.login(r.username(), r.password(), ClientIp.of(req, props.trustProxyHeaders()));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(res.token(), jwt.expiry()).toString())
                .body(res.user());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
                .build();
    }

    @GetMapping("/me")
    public UserInfo me(@AuthenticationPrincipal AuthUser actor) {
        return auth.me(actor);
    }

    @PostMapping("/change-password")
    public ResponseEntity<UserInfo> changePassword(@Valid @RequestBody ChangePasswordRequest r,
                                                   @AuthenticationPrincipal AuthUser actor) {
        AuthService.LoginResult res = auth.changePassword(actor, r.currentPassword(), r.newPassword());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(res.token(), jwt.expiry()).toString())
                .body(res.user());
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(props.cookie().name(), value)
                .httpOnly(true)
                .secure(props.cookie().secure())
                .sameSite(props.cookie().sameSite())
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
