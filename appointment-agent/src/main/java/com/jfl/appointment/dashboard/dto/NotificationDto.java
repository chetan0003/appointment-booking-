package com.jfl.appointment.dashboard.dto;


import com.jfl.appointment.entity.NotificationChannel;
import com.jfl.appointment.entity.NotificationStatus;
import com.jfl.appointment.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationDto(
        Long id,
        Long appointmentId,
        NotificationType type,
        NotificationChannel channel,
        NotificationStatus status,
        LocalDateTime scheduledAt,
        LocalDateTime sentAt,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
