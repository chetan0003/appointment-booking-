package com.jfl.appointment.dashboard.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record CreateNextAppointmentRequest(

        @NotNull
        LocalDate appointmentDate,

        @NotNull
        LocalTime startTime,

        @NotNull
        LocalTime endTime
) {
}
