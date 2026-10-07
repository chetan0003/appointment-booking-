package com.jfl.appointment.repository;

import com.jfl.appointment.entity.AppNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppNotificationRepository extends
        JpaRepository<AppNotification, Long> {

    // Get all notifications
    Page<AppNotification> findByClinic_IdAndRecipient_IdOrderByCreatedAtDesc(
            Long clinicId,
            Long userId,
            Pageable pageable
    );

    // Get unread notifications
    Page<AppNotification> findByClinic_IdAndRecipient_IdAndReadFalseOrderByCreatedAtDesc(
            Long clinicId,
            Long userId,
            Pageable pageable
    );

    // Get unread count
    long countByClinic_IdAndRecipient_IdAndReadFalse(
            Long clinicId,
            Long userId
    );

    // Find notification belonging to current user
    Optional<AppNotification> findByIdAndClinic_IdAndRecipient_Id(
            Long notificationId,
            Long clinicId,
            Long userId
    );

    // Mark all as read
    @Modifying
    @Query("""
        UPDATE AppNotification n
        SET n.read = true,
            n.readAt = CURRENT_TIMESTAMP
        WHERE n.clinic.id = :clinicId
          AND n.recipient.id = :userId
          AND n.read = false
        """)
    int markAllAsRead(
            @Param("clinicId") Long clinicId,
            @Param("userId") Long userId
    );

    List<AppNotification> findByAppointmentIdAndClinicId(
            Long appointmentId,
            Long clinicId
    );
}
