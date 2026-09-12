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
                        name = "uk_clinic_whatsapp_number",
                        columnNames = "whatsapp_number"
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

    /**
     * One WhatsApp configuration per clinic.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "clinic_id",
            nullable = false,
            unique = true
    )
    private Clinic clinic;

    /**
     * WhatsApp number belonging to this clinic.
     *
     * Store in international format.
     *
     * Example:
     * 919876543210
     */
    @Column(
            name = "whatsapp_number",
            nullable = false,
            length = 30
    )
    private String whatsappNumber;

    /**
     * WhatsApp provider.
     *
     * Currently:
     * TWILIO
     *
     * Later you can support:
     * META
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "provider",
            nullable = false,
            length = 20
    )
    private WhatsAppProvider provider;

    /**
     * Twilio parent/master account SID.
     */
    @Column(
            name = "twilio_account_sid",
            length = 100
    )
    private String twilioAccountSid;

    /**
     * Dedicated Twilio subaccount for this clinic.
     */
    @Column(
            name = "twilio_subaccount_sid",
            length = 100
    )
    private String twilioSubaccountSid;

    /**
     * Twilio WhatsApp Sender SID.
     *
     * Example:
     * MGxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     */
    @Column(
            name = "twilio_whatsapp_sender_sid",
            length = 100
    )
    private String twilioWhatsappSenderSid;

    /**
     * WhatsApp Business Account ID.
     */
    @Column(
            name = "waba_id",
            length = 100
    )
    private String wabaId;

    /**
     * Configuration status.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private WhatsAppConfigStatus status =
            WhatsAppConfigStatus.ACTIVE;

    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now =
                LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (status == null) {
            status = WhatsAppConfigStatus.ACTIVE;
        }

        if (provider == null) {
            provider = WhatsAppProvider.TWILIO;
        }
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }
}