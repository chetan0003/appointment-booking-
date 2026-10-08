package com.jfl.appointment.dashboard.service;

import com.jfl.appointment.dto.AppNotificationResponse;
import com.jfl.appointment.entity.*;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.repository.AppNotificationRepository;
import com.jfl.appointment.repository.AppUserRepository;
import com.jfl.appointment.repository.ClinicUserRepository;
import com.jfl.appointment.security.SecurityContextService;

import com.jfl.appointment.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final AppNotificationRepository notificationRepository;
    private final ClinicUserRepository recipientRepository;
    private final SecurityContextService securityContextService;
    private final AppUserRepository appUserRepository;



        // =========================================================
    // CREATE APPOINTMENT NOTIFICATIONS
    // =========================================================

    @Transactional
    public void createAppointmentNotifications(
            Clinic clinic,
            Appointment appointment,
            Long doctorId,
            String patientName,
            String doctorName,
            boolean isWhatsApp
    ) {

        List<Long> recipientIds =
                recipientRepository.findAppointmentRecipients(
                        clinic.getId(),
                        doctorId
                );

        if (recipientIds == null || recipientIds.isEmpty()) {
            log.info(
                    "No notification recipients found for clinicId: {}",
                    clinic.getId()
            );
            return;
        }
        String source = isWhatsApp ? PatientSource.WHATSAPP.name() : PatientSource.DASHBOARD.name();
        String title = "New Appointment("+source+")";

        String message = "New appointment booked for "
                + patientName
                + " with "
                + doctorName;

        List<AppNotification> notifications = recipientIds.stream()
                .distinct()
                .map(userId -> {

                    AppUser recipient = appUserRepository
                            .findById(
                                    userId
                            )
                            .orElseThrow(() ->
                                    new NotFoundException(
                                            "Recipient not found for userId: "
                                                    + userId
                                    )
                            );

                    AppNotification notification = new AppNotification();

                    notification.setClinic(clinic);
                    notification.setRecipient(recipient);
                    notification.setAppointment(appointment);
                    notification.setType(
                            AppNotificationType.APPOINTMENT_CREATED
                    );
                    notification.setTitle(title);
                    notification.setMessage(message);
                    notification.setRead(false);

                    return notification;
                })
                .toList();

        notificationRepository.saveAll(notifications);

        log.info(
                "Created {} appointment notifications for appointmentId: {}",
                notifications.size(),
                appointment.getId()
        );
    }

    @Transactional
    public void rescheduleAppointmentNotifications(
            Clinic clinic,
            Appointment appointment,
            Long doctorId,
            String patientName,
            String doctorName
    ) {

        List<Long> recipientIds =
                recipientRepository.findAppointmentRecipients(
                        clinic.getId(),
                        doctorId
                );

        if (recipientIds == null || recipientIds.isEmpty()) {
            log.info(
                    "No notification recipients found for clinicId: {}",
                    clinic.getId()
            );
            return;
        }

        String title = "Rescheduled Appointment";

        String message = "Rescheduled appointment for "
                + patientName
                + " with Dr. "
                + doctorName;

        List<AppNotification> notifications = recipientIds.stream()
                .distinct()
                .map(userId -> {

                    AppUser recipient = appUserRepository
                            .findById(
                                    userId
                            )
                            .orElseThrow(() ->
                                    new NotFoundException(
                                            "Recipient not found for userId: "
                                                    + userId
                                    )
                            );

                    AppNotification notification = new AppNotification();

                    notification.setClinic(clinic);
                    notification.setRecipient(recipient);
                    notification.setAppointment(appointment);
                    notification.setType(
                            AppNotificationType.APPOINTMENT_CREATED
                    );
                    notification.setTitle(title);
                    notification.setMessage(message);
                    notification.setRead(false);

                    return notification;
                })
                .toList();

        notificationRepository.saveAll(notifications);

        log.info(
                "Created {} appointment notifications for appointmentId: {}",
                notifications.size(),
                appointment.getId()
        );
    }

    @Transactional
    public void cancelledAppointmentNotifications(
            Clinic clinic,
            Appointment appointment,
            Long doctorId,
            String patientName,
            String doctorName
    ) {

        List<Long> recipientIds =
                recipientRepository.findAppointmentRecipients(
                        clinic.getId(),
                        doctorId
                );

        if (recipientIds == null || recipientIds.isEmpty()) {
            log.info(
                    "No notification recipients found for clinicId: {}",
                    clinic.getId()
            );
            return;
        }

        String title = "Cancelled Appointment";

        String message = "Cancelled appointment for "
                + patientName
                + " with Dr. "
                + doctorName;

        List<AppNotification> notifications = recipientIds.stream()
                .distinct()
                .map(userId -> {

                    AppUser recipient = appUserRepository
                            .findById(
                                    userId
                            )
                            .orElseThrow(() ->
                                    new NotFoundException(
                                            "Recipient not found for userId: "
                                                    + userId
                                    )
                            );

                    AppNotification notification = new AppNotification();

                    notification.setClinic(clinic);
                    notification.setRecipient(recipient);
                    notification.setAppointment(appointment);
                    notification.setType(
                            AppNotificationType.APPOINTMENT_CREATED
                    );
                    notification.setTitle(title);
                    notification.setMessage(message);
                    notification.setRead(false);

                    return notification;
                })
                .toList();

        notificationRepository.saveAll(notifications);

        log.info(
                "Created {} appointment notifications for appointmentId: {}",
                notifications.size(),
                appointment.getId()
        );
    }

    // =========================================================
    // GET ALL NOTIFICATIONS
    // =========================================================

    @Transactional(readOnly = true)
    public Page<AppNotificationResponse> getNotifications(
            Long clinicId,
            Pageable pageable
    ) {

        Long userId = securityContextService.getCurrentUserId();

        return notificationRepository
                .findByClinic_IdAndRecipient_IdOrderByCreatedAtDesc(
                        clinicId,
                        userId,
                        pageable
                )
                .map(this::toResponse);
    }

    // =========================================================
    // GET UNREAD NOTIFICATIONS
    // =========================================================

    @Transactional(readOnly = true)
    public Page<AppNotificationResponse> getUnreadNotifications(
            Long clinicId,
            Pageable pageable
    ) {

        Long userId = securityContextService.getCurrentUserId();

        return notificationRepository
                .findByClinic_IdAndRecipient_IdAndReadFalseOrderByCreatedAtDesc(
                        clinicId,
                        userId,
                        pageable
                )
                .map(this::toResponse);
    }

    // =========================================================
    // GET UNREAD COUNT
    // =========================================================

    @Transactional(readOnly = true)
    public long getUnreadCount(Long clinicId) {

        Long userId = securityContextService.getCurrentUserId();

        return notificationRepository
                .countByClinic_IdAndRecipient_IdAndReadFalse(
                        clinicId,
                        userId
                );
    }

    // =========================================================
    // MARK SINGLE NOTIFICATION AS READ
    // =========================================================

    @Transactional
    public void markAsRead(
            Long clinicId,
            Long notificationId
    ) {

        Long userId = securityContextService.getCurrentUserId();

        AppNotification notification =
                notificationRepository
                        .findByIdAndClinic_IdAndRecipient_Id(
                                notificationId,
                                clinicId,
                                userId
                        )
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Notification not found"
                                )
                        );

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());

            notificationRepository.save(notification);
        }
    }

    // =========================================================
    // MARK ALL NOTIFICATIONS AS READ
    // =========================================================

    @Transactional
    public void markAllAsRead(Long clinicId) {

        Long userId = securityContextService.getCurrentUserId();

        notificationRepository.markAllAsRead(
                clinicId,
                userId
        );
    }

    // =========================================================
    // ENTITY TO RESPONSE DTO
    // =========================================================

    private AppNotificationResponse toResponse(
            AppNotification notification
    ) {

        return new AppNotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getAppointment() != null
                        ? notification.getAppointment().getId()
                        : null,
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}