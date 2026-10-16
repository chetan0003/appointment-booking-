package com.jfl.appointment.policy.dto;


import java.util.List;

public record PolicyValidationResponse(
        boolean valid,
        List<String> errors,
        List<String> warnings
) {
}
