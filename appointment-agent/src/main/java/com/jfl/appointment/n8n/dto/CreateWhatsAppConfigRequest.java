package com.jfl.appointment.n8n.dto;


import jakarta.validation.constraints.NotBlank;

public record CreateWhatsAppConfigRequest(

        @NotBlank(message = "Phone number ID is required")
        String phoneNumberId,

        @NotBlank(message = "WABA ID is required")
        String wabaId,

        String businessAccountId,

        @NotBlank(message = "Display phone number is required")
        String displayPhoneNumber,

        @NotBlank(message = "Access token is required")
        String accessToken
) {
}
