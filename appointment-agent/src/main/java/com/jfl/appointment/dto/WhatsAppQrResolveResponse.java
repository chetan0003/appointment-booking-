package com.jfl.appointment.dto;

public record WhatsAppQrResolveResponse(

        boolean valid,

        Long patientId,

        Long clinicId,

        String patientName,

        String message
) {
}