package com.jfl.appointment.dashboard.dto;

import com.jfl.appointment.entity.AppointmentPaymentStatus;
import com.jfl.appointment.entity.AppointmentStatus;
import com.jfl.appointment.entity.QueueStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record AppointmentListItemDto(
        Long id,
        String appointmentCode,
        LocalDate appointmentDate,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal amount,
        AppointmentPaymentStatus paymentStatus,
        AppointmentStatus status,
        String source,
        Long doctorId,
        String clinicName,
        String doctorName,
        Long serviceId,
        String serviceName,
        String patientName,
        String patientPhone,
        Long followUpAppointmentId,
        LocalDate suggestedFollowUpDate,
        Long queueEntryId,
        String queueToken,
        Integer queueNumber,
        LocalDate queueDate,
        QueueStatus queueStatus,
        LocalDateTime checkedInAt,
        LocalDateTime consultationStartedAt,
        LocalDateTime completedAt,
        String rawToken,
        LocalDateTime confirmedAt,
        LocalDateTime CancelledAt
) {}
