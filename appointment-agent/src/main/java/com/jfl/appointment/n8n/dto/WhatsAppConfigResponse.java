package com.jfl.appointment.n8n.dto;


import com.jfl.appointment.entity.WhatsAppConfigStatus;

public record WhatsAppConfigResponse(

        Long id,
        Long clinicId,
        String phoneNumberId,
        String wabaId,
        String businessAccountId,
        String displayPhoneNumber,
        WhatsAppConfigStatus status
) {
}
