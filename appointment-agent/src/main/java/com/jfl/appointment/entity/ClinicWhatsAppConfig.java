package com.jfl.appointment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "clinic_whatsapp_config",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_clinic_whatsapp_clinic",
                        columnNames = "clinic_id"
                ),
                @UniqueConstraint(
                        name = "uk_clinic_whatsapp_phone_number",
                        columnNames = "phone_number_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ClinicWhatsAppConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "clinic_id",
            nullable = false,
            unique = true
    )
    private Clinic clinic;

    @Column(name = "phone_number_id", nullable = false, length = 100)
    private String phoneNumberId;

    @Column(name = "waba_id", nullable = false, length = 100)
    private String wabaId;

    @Column(name = "business_account_id", length = 100)
    private String businessAccountId;

    @Column(name = "display_phone_number", nullable = false, length = 30)
    private String displayPhoneNumber;

    @Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
    private String accessToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WhatsAppConfigStatus status = WhatsAppConfigStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ClinicWhatsAppConfig(Clinic clinic, String phoneNumberId, String wabaId, String businessAccountId, String displayPhoneNumber, String token, WhatsAppConfigStatus whatsAppConfigStatus, LocalDateTime now, LocalDateTime now1) {
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = WhatsAppConfigStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}