package com.jfl.appointment.repository;

import com.jfl.appointment.entity.Appointment;
import com.jfl.appointment.entity.AppointmentStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


public final class AppointmentSpecification {

    private AppointmentSpecification() {
    }

    public static Specification<Appointment> forDashboard(
            Long clinicId,
            LocalDate from,
            LocalDate to,
            Long doctorId,
            Long serviceId,
            AppointmentStatus status) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // Required
            predicates.add(
                    cb.equal(
                            root.get("clinic").get("id"),
                            clinicId
                    )
            );

            // Optional date filters
            if (from != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("appointmentDate"),
                                from
                        )
                );
            }

            if (to != null) {
                predicates.add(
                        cb.lessThanOrEqualTo(
                                root.get("appointmentDate"),
                                to
                        )
                );
            }

            // Optional doctor
            if (doctorId != null) {
                predicates.add(
                        cb.equal(
                                root.get("doctor").get("id"),
                                doctorId
                        )
                );
            }

            // Optional service
            if (serviceId != null) {
                predicates.add(
                        cb.equal(
                                root.get("service").get("id"),
                                serviceId
                        )
                );
            }

            // Optional status
            if (status != null) {
                predicates.add(
                        cb.equal(
                                root.get("status"),
                                status
                        )
                );
            }

            query.orderBy(
                    cb.asc(root.get("appointmentDate")),
                    cb.asc(root.get("startTime"))
            );

            return cb.and(
                    predicates.toArray(new Predicate[0])
            );
        };
    }
}
