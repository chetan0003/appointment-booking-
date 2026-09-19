package com.jfl.appointment.n8n.dto;

// Everything the QR flow needs from a single lookup: which clinic this is,
// what service to book by default (no service-selection step in that flow),
// and the Twilio credentials to reply with - all resolved from the `To`
// number on every inbound message.
public record ClinicWhatsappConfigDto(
        Long clinicId,
        String clinicName,
        Long defaultServiceId,
        String twilioSubaccountSid,
        String twilioAuthToken,
        String twilioWhatsappSender
) {}
