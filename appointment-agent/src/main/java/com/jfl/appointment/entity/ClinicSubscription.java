package com.jfl.appointment.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(
        name = "clinic_subscription",
        indexes = {
                @Index(
                        name = "idx_clinic_subscription_clinic",
                        columnList = "clinic_id"
                ),
                @Index(
                        name = "idx_clinic_subscription_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ClinicSubscription extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "clinic_id",
            nullable = false,
            unique = true
    )
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "plan_id",
            nullable = false
    )
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private SubscriptionStatus status;

    @Column(
            name = "start_date"
    )
    private LocalDate startDate;


    @Column(name = "trial_end_date")
    private LocalDate trialEndDate;

    @Column(
            name = "end_date"
    )
    private LocalDate endDate;

    @Column(
            name = "auto_renew",
            nullable = false
    )
    private boolean autoRenew = false;
}
