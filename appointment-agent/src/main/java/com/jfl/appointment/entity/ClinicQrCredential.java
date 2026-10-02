package com.jfl.appointment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "clinic_qr_credential",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_clinic_qr_token_hash",
                        columnNames = "token_hash"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ClinicQrCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "encrypted_token", length = 500)
    private String encryptedToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClinicQrStatus status = ClinicQrStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
