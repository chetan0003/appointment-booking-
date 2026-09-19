package com.jfl.appointment.repository;

import com.jfl.appointment.entity.PatientQrCredential;
import com.jfl.appointment.entity.PatientQrStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientQrCredentialRepository
        extends JpaRepository<PatientQrCredential, Long> {

    Optional<PatientQrCredential> findByTokenHashAndStatus(
            String tokenHash,
            PatientQrStatus status
    );

    List<PatientQrCredential> findByPatientIdAndStatus(
            Long patientId,
            PatientQrStatus status
    );

    Optional<PatientQrCredential> findFirstByPatientIdAndStatusOrderByCreatedAtDesc(
            Long patientId,
            PatientQrStatus status
    );


}