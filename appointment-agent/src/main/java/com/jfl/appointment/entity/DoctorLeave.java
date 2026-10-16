package com.jfl.appointment.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "doctor_leaves",
        indexes = {
                @Index(
                        name = "idx_doctor_leaves_doctor_date",
                        columnList = "doctor_id, leave_date, active"
                )
        }
)
@Getter
@Setter
public class DoctorLeave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "doctor_id",
            nullable = false
    )
    private Doctor doctor;

    @Column(
            name = "leave_date",
            nullable = false
    )
    private LocalDate leaveDate;

    /*
     * NULL start/end = full-day leave
     *
     * Example:
     * 09:00 - 13:00 = partial leave
     */
    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "reason")
    private String reason;

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
