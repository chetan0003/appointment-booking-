package com.jfl.appointment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "clinic_holidays",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_clinic_holiday",
                        columnNames = {
                                "clinic_id",
                                "holiday_date"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_clinic_holidays_clinic_date",
                        columnList = "clinic_id, holiday_date, active"
                )
        }
)
@Getter
@Setter
public class ClinicHoliday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "clinic_id",
            nullable = false
    )
    private Clinic clinic;

    @Column(
            name = "holiday_date",
            nullable = false
    )
    private LocalDate holidayDate;

    @Column(
            name = "name",
            nullable = false
    )
    private String name;

    @Column(
            name = "active",
            nullable = false
    )
    private boolean active = true;

    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt = LocalDateTime.now();
}