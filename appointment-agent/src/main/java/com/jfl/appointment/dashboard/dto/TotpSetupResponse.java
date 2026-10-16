package com.jfl.appointment.dashboard.dto;

public record TotpSetupResponse(
        String secret,
        String otpauthUri
) {}

