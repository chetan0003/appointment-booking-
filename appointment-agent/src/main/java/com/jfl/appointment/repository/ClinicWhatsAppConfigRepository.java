package com.jfl.appointment.repository;

import com.jfl.appointment.entity.ClinicWhatsAppConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClinicWhatsAppConfigRepository
        extends JpaRepository<ClinicWhatsAppConfig, Long> {

    Optional<ClinicWhatsAppConfig> findByClinicId(Long clinicId);

    Optional<ClinicWhatsAppConfig> findByPhoneNumberId(String phoneNumberId);

    boolean existsByClinicId(Long clinicId);

    boolean existsByPhoneNumberId(String phoneNumberId);
}