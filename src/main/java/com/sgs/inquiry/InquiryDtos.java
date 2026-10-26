package com.sgs.inquiry;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class InquiryDtos {
    private InquiryDtos() {}

    /** website = honeypot field: real visitors never fill it (hide it with CSS). turnstileToken = Cloudflare captcha. */
    public record InquiryRequest(@NotBlank @Size(max = 100) String name,
                                 @Email @Size(max = 150) String email,
                                 @Pattern(regexp = "^[0-9+()\\-\\s]{7,25}$", message = "Invalid phone number") String phone,
                                 @Size(max = 150) String serviceInterest,
                                 @NotBlank @Size(max = 3000) String message,
                                 @Size(max = 200) String website,
                                 @Size(max = 4096) String turnstileToken) {}

    public record InquiryDto(Long id, String name, String email, String phone, String serviceInterest, String message,
                             InquiryStatus status, Instant createdAt) {}

    public record StatusRequest(@NotNull InquiryStatus status) {}

    public record SubmitResult(String message) {}
}
