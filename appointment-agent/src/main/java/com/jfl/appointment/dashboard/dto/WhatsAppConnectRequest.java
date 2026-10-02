package com.jfl.appointment.dashboard.dto;

public record WhatsAppConnectRequest(
        String code,
        Long clinicId

) {}
