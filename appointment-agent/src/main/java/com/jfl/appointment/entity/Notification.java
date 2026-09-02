package com.jfl.appointment.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notification",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_notification_appointment_type_channel",
                        columnNames = {
                                "appointment_id",
                                "type",
                                "channel"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ---------------------------------------------
    // Appointment
    // ---------------------------------------------

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "appointment_id",
            nullable = false
    )
    private Appointment appointment;

    // ---------------------------------------------
    // Notification type
    // ---------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private NotificationType type;

    // ---------------------------------------------
    // Channel
    // ---------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    @Builder.Default
    private NotificationChannel channel =
            NotificationChannel.WHATSAPP;

    // ---------------------------------------------
    // Status
    // ---------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    @Builder.Default
    private NotificationStatus status =
            NotificationStatus.PENDING;

    // ---------------------------------------------
    // Scheduling
    // ---------------------------------------------

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    // ---------------------------------------------
    // Sending
    // ---------------------------------------------

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    // ---------------------------------------------
    // Audit
    // ---------------------------------------------

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    @Builder.Default
    private LocalDateTime createdAt =
            LocalDateTime.now();

    @Column(
            name = "updated_at",
            nullable = false
    )
    @Builder.Default
    private LocalDateTime updatedAt =
            LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
