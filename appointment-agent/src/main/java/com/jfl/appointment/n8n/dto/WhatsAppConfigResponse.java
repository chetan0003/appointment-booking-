package com.jfl.appointment.n8n.dto;


import com.jfl.appointment.entity.WhatsAppConfigStatus;
import com.jfl.appointment.entity.WhatsAppProvider;

public record WhatsAppConfigResponse(

        Long id,

        Long clinicId,

        String whatsappNumber,

        WhatsAppProvider provider,

        String twilioAccountSid,

        String twilioSubaccountSid,

        String twilioWhatsappSenderSid,

        String wabaId,

        WhatsAppConfigStatus status
) {
}