package com.jfl.appointment.dto;

import jakarta.validation.constraints.NotNull;

public record VerifyPaymentRequest(

        @NotNull
        Boolean approved,

        String rejectionReason
) {
}
