package com.jfl.appointment.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "patient_qr_credential",
        indexes = {
                @Index(
                        name = "idx_patient_qr_token_hash",
                        columnList = "token_hash"
                ),
                @Index(
                        name = "idx_patient_qr_patient_id",
                        columnList = "patient_id"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_patient_qr_token_hash",
                        columnNames = "token_hash"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class PatientQrCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /**
     * Never store the raw token.
     * Store SHA-256 hash only.
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "encrypted_token", nullable = false, length = 500)
    private String encryptedToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PatientQrStatus status = PatientQrStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        if (status == null) {
            status = PatientQrStatus.ACTIVE;
        }
    }
}
