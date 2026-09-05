package com.jfl.appointment.dashboard.dto;


import com.jfl.appointment.entity.NotificationType;

import java.time.LocalDate;
import java.time.LocalTime;

public record NotificationDueDto(
        Long notificationId,
        Long appointmentId,
        NotificationType type,
        String patientName,
        String patientPhone,
        String doctorName,
        String serviceName,
        LocalDate appointmentDate,
        LocalTime startTime
) {}
