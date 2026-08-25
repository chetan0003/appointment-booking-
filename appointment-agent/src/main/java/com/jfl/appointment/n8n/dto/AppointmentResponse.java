package com.jfl.appointment.n8n.dto;

import com.jfl.appointment.entity.AppointmentStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentResponse(
        String appointmentId,
        AppointmentStatus status,
        LocalDate appointmentDate,
        LocalTime startTime,
        LocalTime endTime,
        String doctorName,
        String serviceName
) {}
