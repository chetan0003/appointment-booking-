package com.jfl.appointment.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "clinic_queue_entry",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_queue_clinic_date_number",
                        columnNames = {"clinic_id", "queue_date", "queue_number"}
                ),
                @UniqueConstraint(
                        name = "uq_queue_appointment",
                        columnNames = {"appointment_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ClinicQueueEntry extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @Column(name = "queue_date", nullable = false)
    private LocalDate queueDate;

    @Column(name = "queue_number", nullable = false)
    private Integer queueNumber;

    @Column(name = "queue_token", nullable = false)
    private String queueToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "queue_status", nullable = false)
    private QueueStatus queueStatus = QueueStatus.WAITING;

    @Column(name = "checked_in_at", nullable = false)
    private LocalDateTime checkedInAt;

    private LocalDateTime calledAt;

    private LocalDateTime cancelledAt;

    private LocalDateTime consultationStartedAt;

    private LocalDateTime completedAt;
}
