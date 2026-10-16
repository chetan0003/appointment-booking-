package com.jfl.appointment.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "subscription_plan")
@Getter
@Setter
@NoArgsConstructor
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            unique = true,
            length = 30
    )
    private SubscriptionPlanCode code;

    @Column(
            nullable = false,
            length = 100
    )
    private String name;

    @Column(length = 500)
    private String description;

    @Column(
            name = "monthly_price",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal monthlyPrice;

    @Column(
            name = "yearly_price",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal yearlyPrice;

    @Column(
            name = "max_doctors",
            nullable = false
    )
    private Integer maxDoctors;

    @Column(
            name = "max_staff",
            nullable = false
    )
    private Integer maxStaff;

    @Column(
            name = "max_appointments_per_month",
            nullable = false
    )
    private Integer maxAppointmentsPerMonth;

    @Column(
            name = "max_patients",
            nullable = false
    )
    private Integer maxPatients;

    @Column(
            name = "ai_receptionist_enabled",
            nullable = false
    )
    private boolean aiReceptionistEnabled;

    @Column(
            name = "whatsapp_enabled",
            nullable = false
    )
    private boolean whatsappEnabled;

    @Column(
            name = "analytics_enabled",
            nullable = false
    )
    private boolean analyticsEnabled;

    @Column(
            nullable = false
    )
    private boolean active = true;
}
