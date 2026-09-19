package com.jfl.appointment.dashboard.dto;

import java.time.LocalDate;

public record WeeklyAppointmentDto(
        String day,
        LocalDate date,
        long appointmentCount
) {
}
