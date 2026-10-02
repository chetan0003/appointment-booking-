package com.jfl.appointment.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmitPaymentRequest(

        @NotNull
        Long planId,

        @NotBlank
        String transactionId
) {
}
