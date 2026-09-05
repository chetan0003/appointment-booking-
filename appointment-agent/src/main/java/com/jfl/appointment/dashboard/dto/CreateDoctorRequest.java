package com.jfl.appointment.dashboard.dto;



public record CreateDoctorRequest(
        String name,
        String specialization,
        Long serviceId
) {
}
