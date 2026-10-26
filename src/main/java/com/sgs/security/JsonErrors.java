package com.sgs.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgs.common.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.Instant;

final class JsonErrors {
    private JsonErrors() {}

    static void write(ObjectMapper mapper, HttpServletResponse res, int status, String code, String message) throws IOException {
        if (res.isCommitted()) return;
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        mapper.writeValue(res.getWriter(), new ErrorResponse(Instant.now(), status, code, message, null));
    }
}
