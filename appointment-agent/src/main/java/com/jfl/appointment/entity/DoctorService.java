package com.jfl.appointment.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "doctor_service",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_doctor_service",
                        columnNames = {"doctor_id", "service_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceOffering service;
}