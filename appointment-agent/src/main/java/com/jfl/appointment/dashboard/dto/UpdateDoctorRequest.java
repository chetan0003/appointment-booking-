package com.jfl.appointment.dashboard.dto;

public record UpdateDoctorRequest(
        String name,
        String specialization,
        Long serviceId,
        boolean active
) {}
