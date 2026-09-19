package com.jfl.appointment.dashboard.dto;

import com.jfl.appointment.entity.AppointmentStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentListItemDto(
        Long id,
        String appointmentCode,
        LocalDate appointmentDate,
        LocalTime startTime,
        LocalTime endTime,
        AppointmentStatus status,
        Long doctorId,
        String doctorName,
        Long serviceId,
        String serviceName,
        String patientName,
        String patientPhone,
        Long followUpAppointmentId,
        LocalDate suggestedFollowUpDate
) {}
