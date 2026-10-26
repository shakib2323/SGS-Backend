package com.sgs.inquiry;

import com.sgs.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

/** Cloudflare Turnstile server-side check. Disabled (always passes) when TURNSTILE_SECRET is empty. */
@Slf4j
@Service
public class TurnstileService {

    private static final String VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    private final String secret;
    private final RestClient client;

    public TurnstileService(AppProperties props) {
        this.secret = props.turnstile() == null ? null : props.turnstile().secret();
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(3000);
        f.setReadTimeout(3000);
        this.client = RestClient.builder().requestFactory(f).build();
    }

    public boolean enabled() {
        return secret != null && !secret.isBlank();
    }

    /** Fails closed: network errors count as "not verified". */
    public boolean verify(String token, String ip) {
        if (!enabled()) return true;
        if (token == null || token.isBlank()) return false;
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("secret", secret);
            form.add("response", token);
            if (ip != null) form.add("remoteip", ip);
            Map<?, ?> body = client.post().uri(VERIFY_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
            return body != null && Boolean.TRUE.equals(body.get("success"));
        } catch (RuntimeException e) {
            log.warn("Turnstile verification failed: {}", e.getMessage());
            return false;
        }
    }
}
