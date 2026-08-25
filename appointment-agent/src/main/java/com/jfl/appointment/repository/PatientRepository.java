package com.jfl.appointment.repository;

import com.jfl.appointment.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByClinicIdAndWhatsappNumber(Long clinicId, String whatsappNumber);

    Optional<Patient> findByClinicIdAndWhatsappNumberAndName(
            Long clinicId,
            String whatsappNumber,
            String name
    );

    long countByClinicId(Long clinicId);
}
