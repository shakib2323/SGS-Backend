package com.sgs.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** JSON 401/403 for requests rejected by the security filter chain. */
@Component
@RequiredArgsConstructor
public class RestAuthEntryPoint implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper mapper;

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) throws IOException {
        Object code = req.getAttribute(JwtAuthFilter.ATTR_AUTH_ERROR);
        if ("ACCOUNT_SUSPENDED".equals(code)) {
            JsonErrors.write(mapper, res, 401, "ACCOUNT_SUSPENDED", "Your account is suspended. Contact the administrator.");
        } else {
            JsonErrors.write(mapper, res, 401, "UNAUTHORIZED", "Authentication required");
        }
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex) throws IOException {
        JsonErrors.write(mapper, res, 403, "FORBIDDEN", "You are not allowed to do this");
    }
}
