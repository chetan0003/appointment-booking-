package com.jfl.appointment.n8n.dto;


import jakarta.validation.constraints.NotBlank;
public record CreateWhatsAppConfigRequest(

        @NotBlank
        String whatsappNumber,

        @NotBlank
        String twilioAccountSid,

        String twilioSubaccountSid,

        @NotBlank
        String twilioWhatsappSenderSid,

        String wabaId

) {
}
