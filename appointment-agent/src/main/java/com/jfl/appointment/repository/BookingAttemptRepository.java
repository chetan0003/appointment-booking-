package com.jfl.appointment.repository;

import com.jfl.appointment.entity.BookingAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface BookingAttemptRepository extends JpaRepository<BookingAttempt, Long> {

    long countByWhatsappNumberAndClinicIdAndCreatedAtAfter(
            String whatsappNumber,
            Long clinicId,
            Instant createdAt);
}
