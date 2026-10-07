package com.jfl.appointment.repository;


import com.jfl.appointment.entity.AppointmentQrCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppointmentQrCredentialRepository
        extends JpaRepository<AppointmentQrCredential, Long> {

    Optional<AppointmentQrCredential> findByAppointmentId(
            Long appointmentId
    );

    Optional<AppointmentQrCredential> findByTokenHash(
            String tokenHash
    );
}
