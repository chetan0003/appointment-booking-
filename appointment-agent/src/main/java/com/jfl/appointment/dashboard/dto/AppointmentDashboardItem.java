package com.jfl.appointment.dashboard.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentDashboardItem(
        Long id,
        String patientName,
        String doctorName,
        String serviceName,
        LocalDate appointmentDate,
        LocalTime startTime,
        String status
) {
}
