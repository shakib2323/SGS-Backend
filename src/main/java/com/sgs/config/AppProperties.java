package com.sgs.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String timezone,
        boolean trustProxyHeaders,
        @Valid Jwt jwt,
        @Valid AuthCookie cookie,
        Cors cors,
        @Valid Storage storage,
        Bootstrap bootstrap,
        Limits limits,
        Turnstile turnstile) {

    public record Jwt(@NotBlank @Size(min = 32, message = "JWT secret must be at least 32 characters") String secret,
                      Duration expiry) {}

    public record AuthCookie(@NotBlank String name, boolean secure, @NotBlank String sameSite) {}

    public record Cors(List<String> allowedOrigins) {
        public Cors {
            allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins;
        }
    }

    public record Storage(@NotBlank String root, @NotBlank String publicBaseUrl, boolean serveFiles, long maxFileBytes) {}

    public record Bootstrap(String adminUsername, String adminPassword, String adminFullName) {}

    public record Limits(int serviceImages, int loginMaxFailures, Duration loginWindow,
                         int inquiriesPerHour, BigDecimal maxBankAmount) {}

    public record Turnstile(String secret) {}
}
