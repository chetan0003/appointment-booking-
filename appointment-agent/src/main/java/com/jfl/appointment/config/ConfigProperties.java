package com.jfl.appointment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hola")
public record ConfigProperties(
        App app,
        Booking booking,
        Admin admin
) {

    public record App (
            String holaMdBaseUrl,
            String holaMdAppBaseUrl

    ) {
    }
    public record Booking(
            int maxSuccessPerPhonePerClinicPerDay,
            int maxAttemptsPerPhonePer10Min,
            int sessionTtlMinutes,
            int maxSuccessPerSession
    ) {
    }

    public record Admin(
            String username,
            String email,
            String password,
            String firstName,
            String lastName
    ) {
    }
}
