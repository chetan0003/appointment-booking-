package com.jfl.appointment.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        ErrorResponse error
) {

    public static <T> ApiResponse<T> success(
            String message,
            T data) {

        return new ApiResponse<>(
                true,
                message,
                data,
                null
        );
    }

    public static <T> ApiResponse<T> error(
            String message,
            ErrorResponse error) {

        return new ApiResponse<>(
                false,
                message,
                null,
                error
        );
    }
}
