package com.jfl.appointment.dto;

import java.time.LocalDateTime;

public record PatientQrResponse(

        Long patientId,

        Long clinicId,

        Long qrCredentialId,

        String status,

        String whatsappNumber,

        String whatsappUrl,

        String qrImageBase64,

        LocalDateTime createdAt,

        LocalDateTime expiresAt
) {
}