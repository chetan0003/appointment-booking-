package com.jfl.appointment.exception;

import com.jfl.appointment.dashboard.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // =========================================================
    // 404 - Resource Not Found
    // =========================================================

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            NotFoundException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(
                        new ApiErrorResponse(
                                "NOT_FOUND",
                                ex.getMessage(),
                                LocalDateTime.now(),
                                request.getRequestURI()
                        )
                );
    }

    // =========================================================
    // 409 - Slot Unavailable
    // =========================================================

    @ExceptionHandler(SlotUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleSlotUnavailable(
            SlotUnavailableException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        new ApiErrorResponse(
                                "SLOT_UNAVAILABLE",
                                ex.getMessage(),
                                LocalDateTime.now(),
                                request.getRequestURI()
                        )
                );
    }

    // =========================================================
    // 409 - Database Constraint Violation
    // =========================================================

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        log.error(
                "Database constraint violation. path={}",
                request.getRequestURI(),
                ex
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        new ApiErrorResponse(
                                "CONFLICT",
                                "The requested operation could not be completed because the data already exists or conflicts with existing data.",
                                LocalDateTime.now(),
                                request.getRequestURI()
                        )
                );
    }

    // =========================================================
    // 400 - Bean Validation
    // =========================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String message =
                ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(error ->
                                error.getField()
                                        + ": "
                                        + error.getDefaultMessage()
                        )
                        .collect(Collectors.joining(", "));

        if (message.isBlank()) {
            message = "Invalid request";
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        new ApiErrorResponse(
                                "VALIDATION_ERROR",
                                message,
                                LocalDateTime.now(),
                                request.getRequestURI()
                        )
                );
    }

    // =========================================================
    // 400 - Illegal Argument
    // =========================================================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        new ApiErrorResponse(
                                "BAD_REQUEST",
                                ex.getMessage(),
                                LocalDateTime.now(),
                                request.getRequestURI()
                        )
                );
    }

    // =========================================================
    // 403 - Access Denied
    // =========================================================

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(
                        new ApiErrorResponse(
                                "FORBIDDEN",
                                "You do not have permission to perform this operation.",
                                LocalDateTime.now(),
                                request.getRequestURI()
                        )
                );
    }

    // =========================================================
    // 401 - Authentication Failure
    // =========================================================

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(
            AuthenticationException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(
                        new ApiErrorResponse(
                                "UNAUTHORIZED",
                                "Authentication is required.",
                                LocalDateTime.now(),
                                request.getRequestURI()
                        )
                );
    }

    // =========================================================
    // 500 - Unexpected Exception
    // =========================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error(
                "Unhandled exception. path={}",
                request.getRequestURI(),
                ex
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        new ApiErrorResponse(
                                "INTERNAL_SERVER_ERROR",
                                "Something went wrong. Please try again later.",
                                LocalDateTime.now(),
                                request.getRequestURI()
                        )
                );
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(
            ConflictException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        new ApiErrorResponse(
                                "CONFLICT",
                                ex.getMessage(),
                                LocalDateTime.now(),
                                request.getRequestURI()
                        )
                );
    }
}