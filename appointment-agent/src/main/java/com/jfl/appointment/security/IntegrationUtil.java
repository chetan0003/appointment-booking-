package com.jfl.appointment.security;

import java.util.UUID;

public class IntegrationUtil {

    public static String generateAppointmentCode(String idempotencyKey) {
        if (idempotencyKey != null) {
            return "IDEMP-" + idempotencyKey;
        }
        return "APT-" + System.currentTimeMillis() % 1_000_000 + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
