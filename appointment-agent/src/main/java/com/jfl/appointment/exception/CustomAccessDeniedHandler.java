package com.jfl.appointment.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfl.appointment.dashboard.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public CustomAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {

        ApiErrorResponse errorResponse =
                new ApiErrorResponse(
                        "FORBIDDEN",
                        "You do not have permission to perform this operation.",
                        LocalDateTime.now(),
                        request.getRequestURI()
                );

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        response.getWriter().write(
                objectMapper.writeValueAsString(errorResponse)
        );
    }
}