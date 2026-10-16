package com.jfl.appointment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "appointment_qr_credential",
        indexes = {
                @Index(
                        name = "idx_appointment_qr_token_hash",
                        columnList = "token_hash",
                        unique = true
                ),
                @Index(
                        name = "idx_appointment_qr_appointment",
                        columnList = "appointment_id"
                )
        }
)
@Getter
@Setter
public class AppointmentQrCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "appointment_id",
            nullable = false,
            unique = true
    )
    private Appointment appointment;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "encrypted_token", nullable = false)
    private String encryptedToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentQrStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime scannedAt;
}
