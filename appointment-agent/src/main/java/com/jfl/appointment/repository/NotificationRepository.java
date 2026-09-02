package com.jfl.appointment.repository;

import com.jfl.appointment.entity.Notification;
import com.jfl.appointment.entity.NotificationChannel;
import com.jfl.appointment.entity.NotificationStatus;
import com.jfl.appointment.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    boolean existsByAppointmentIdAndTypeAndChannel(
            Long appointmentId,
            NotificationType type,
            NotificationChannel channel
    );

    List<Notification> findByStatusAndScheduledAtLessThanEqual(
            NotificationStatus status,
            LocalDateTime scheduledAt
    );


    List<Notification> findByAppointmentIdOrderByCreatedAtDesc(
            Long appointmentId
    );

}
