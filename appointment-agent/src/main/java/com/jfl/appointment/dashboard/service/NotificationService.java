package com.jfl.appointment.dashboard.service;

import com.jfl.appointment.dashboard.dto.NotificationDto;
import com.jfl.appointment.entity.*;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.repository.AppointmentRepository;
import com.jfl.appointment.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AppointmentRepository appointmentRepository;

    // =========================================================
    // Create Notification
    // =========================================================

    @Transactional
    public NotificationDto createNotification(
            Long appointmentId,
            NotificationType type,
            NotificationChannel channel,
            LocalDateTime scheduledAt) {

        Appointment appointment =
                appointmentRepository
                        .findById(appointmentId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Appointment not found: "
                                                + appointmentId
                                )
                        );

        // -----------------------------------------------------
        // Prevent duplicate notification
        // -----------------------------------------------------

        boolean alreadyExists =
                notificationRepository
                        .existsByAppointmentIdAndTypeAndChannel(
                                appointmentId,
                                type,
                                channel
                        );

        if (alreadyExists) {

            log.info(
                    "Notification already exists. appointmentId={}, type={}, channel={}",
                    appointmentId,
                    type,
                    channel
            );

            return notificationRepository
                    .findByAppointmentIdOrderByCreatedAtDesc(
                            appointmentId
                    )
                    .stream()
                    .filter(n ->
                            n.getType() == type
                                    && n.getChannel() == channel
                    )
                    .findFirst()
                    .map(this::toDto)
                    .orElseThrow();
        }

        // -----------------------------------------------------
        // Create notification
        // -----------------------------------------------------

        Notification notification =
                Notification.builder()
                        .appointment(appointment)
                        .type(type)
                        .channel(channel)
                        .status(NotificationStatus.PENDING)
                        .scheduledAt(scheduledAt)
                        .build();

        Notification saved =
                notificationRepository.save(notification);

        log.info(
                "Notification created. id={}, appointmentId={}, type={}, channel={}, scheduledAt={}",
                saved.getId(),
                appointmentId,
                type,
                channel,
                scheduledAt
        );

        return toDto(saved);
    }

    // =========================================================
    // Booking Confirmation
    // =========================================================

    @Transactional
    public NotificationDto createBookingConfirmation(
            Appointment appointment) {

        return createNotification(
                appointment.getId(),
                NotificationType.BOOKING_CONFIRMATION,
                NotificationChannel.WHATSAPP,
                LocalDateTime.now()
        );
    }

    // =========================================================
    // Rescheduled Notification
    // =========================================================

    @Transactional
    public NotificationDto createRescheduledNotification(
            Appointment appointment) {

        return createNotification(
                appointment.getId(),
                NotificationType.RESCHEDULED,
                NotificationChannel.WHATSAPP,
                LocalDateTime.now()
        );
    }

    // =========================================================
    // Follow-up Suggested Notification
    // =========================================================

    @Transactional
    public NotificationDto createFollowUpSuggestedNotification(
            Appointment appointment) {

        return createNotification(
                appointment.getId(),
                NotificationType.FOLLOW_UP_SUGGESTED,
                NotificationChannel.WHATSAPP,
                LocalDateTime.now()
        );
    }

    // =========================================================
    // 24 Hour Reminder
    // =========================================================

    @Transactional
    public NotificationDto create24HourReminder(
            Appointment appointment,
            LocalDateTime scheduledAt) {

        return createNotification(
                appointment.getId(),
                NotificationType.REMINDER_24H,
                NotificationChannel.WHATSAPP,
                scheduledAt
        );
    }

    // =========================================================
    // Get Appointment Notifications
    // =========================================================

    @Transactional(readOnly = true)
    public List<NotificationDto> getAppointmentNotifications(
            Long appointmentId) {

        if (!appointmentRepository.existsById(appointmentId)) {

            throw new NotFoundException(
                    "Appointment not found: " + appointmentId
            );
        }

        return notificationRepository
                .findByAppointmentIdOrderByCreatedAtDesc(
                        appointmentId
                )
                .stream()
                .map(this::toDto)
                .toList();
    }

    // =========================================================
    // Get Pending Notifications
    // =========================================================

    @Transactional(readOnly = true)
    public List<Notification> getPendingNotifications() {

        return notificationRepository
                .findByStatusAndScheduledAtLessThanEqual(
                        NotificationStatus.PENDING,
                        LocalDateTime.now()
                );
    }

    // =========================================================
    // Mark Sent
    // =========================================================

    @Transactional
    public void markAsSent(Long notificationId) {

        Notification notification =
                notificationRepository
                        .findById(notificationId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Notification not found: "
                                                + notificationId
                                )
                        );

        notification.setStatus(
                NotificationStatus.SENT
        );

        notification.setSentAt(
                LocalDateTime.now()
        );

        notificationRepository.save(notification);

        log.info(
                "Notification marked as SENT. notificationId={}",
                notificationId
        );
    }

    // =========================================================
    // Mark Failed
    // =========================================================

    @Transactional
    public void markAsFailed(
            Long notificationId,
            String errorMessage) {

        Notification notification =
                notificationRepository
                        .findById(notificationId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Notification not found: "
                                                + notificationId
                                )
                        );

        notification.setStatus(
                NotificationStatus.FAILED
        );

        notification.setErrorMessage(
                errorMessage
        );

        notificationRepository.save(notification);

        log.error(
                "Notification marked as FAILED. notificationId={}, error={}",
                notificationId,
                errorMessage
        );
    }

    // =========================================================
    // Mapper
    // =========================================================

    private NotificationDto toDto(
            Notification notification) {

        return new NotificationDto(
                notification.getId(),
                notification.getAppointment().getId(),
                notification.getType(),
                notification.getChannel(),
                notification.getStatus(),
                notification.getScheduledAt(),
                notification.getSentAt(),
                notification.getErrorMessage(),
                notification.getCreatedAt(),
                notification.getUpdatedAt()
        );
    }
}
