package com.jfl.appointment.repository;

import com.jfl.appointment.entity.ClinicQrCredential;
import com.jfl.appointment.entity.ClinicQrStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClinicQrCredentialRepository
        extends JpaRepository<ClinicQrCredential, Long> {

    Optional<ClinicQrCredential>
    findFirstByClinicIdAndStatusOrderByCreatedAtDesc(
            Long clinicId,
            ClinicQrStatus status
    );

    Optional<ClinicQrCredential>
    findByTokenHashAndStatus(
            String tokenHash,
            ClinicQrStatus status
    );
}
