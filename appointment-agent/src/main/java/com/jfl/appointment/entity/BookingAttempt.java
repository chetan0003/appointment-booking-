package com.jfl.appointment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "booking_attempt", indexes = {
        @Index(name = "idx_booking_attempt_phone_clinic_created",
                columnList = "whatsapp_number, clinic_id, created_at")
})
@Getter
@Setter
public class BookingAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clinic_id", nullable = false)
    private Long clinicId;

    @Column(name = "whatsapp_number", nullable = false, length = 32)
    private String whatsappNumber;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    // getters / setters
}
