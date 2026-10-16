package com.jfl.appointment.dto;

import java.time.LocalDateTime;

public record AppointmentQrCredentialResult(
        Long credentialId,
        String token,
        LocalDateTime expiresAt
) {
}
