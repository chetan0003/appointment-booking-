package com.jfl.appointment.repository;

import com.jfl.appointment.entity.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClinicRepository extends JpaRepository<Clinic, Long> {

    boolean existsByNameIgnoreCaseOrWhatsappNumber(
            String name,
            String whatsappNumber
    );

    // Resolves which clinic an inbound Twilio message belongs to, via the
    // `To` number on every single message - not just the QR's first one.
    Optional<Clinic> findByWhatsappNumber(String whatsappNumber);
}
