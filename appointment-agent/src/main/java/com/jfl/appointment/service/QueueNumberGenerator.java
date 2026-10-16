package com.jfl.appointment.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
@RequiredArgsConstructor
public class QueueNumberGenerator {

    private final EntityManager entityManager;

    public int nextNumber(Long clinicId, LocalDate queueDate) {

        Object result = entityManager.createNativeQuery("""
                INSERT INTO clinic_queue_counter (
                    clinic_id,
                    queue_date,
                    last_number
                )
                VALUES (:clinicId, :queueDate, 1)
                ON CONFLICT (clinic_id, queue_date)
                DO UPDATE SET
                    last_number =
                        clinic_queue_counter.last_number + 1
                RETURNING last_number
                """)
                .setParameter("clinicId", clinicId)
                .setParameter("queueDate", queueDate)
                .getSingleResult();

        return ((Number) result).intValue();
    }
}
