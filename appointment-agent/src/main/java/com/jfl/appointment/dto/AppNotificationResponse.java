package com.jfl.appointment.dto;

import com.jfl.appointment.entity.AppNotificationType;

import java.time.LocalDateTime;

public record AppNotificationResponse(
        Long id,
        AppNotificationType type,
        String title,
        String message,
        Long appointmentId,
        boolean read,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {}
