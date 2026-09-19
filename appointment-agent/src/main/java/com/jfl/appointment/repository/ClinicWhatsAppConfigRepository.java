package com.jfl.appointment.repository;

import com.jfl.appointment.entity.ClinicWhatsAppConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClinicWhatsAppConfigRepository
        extends JpaRepository<ClinicWhatsAppConfig, Long> {

    Optional<ClinicWhatsAppConfig> findByClinicId(Long clinicId);


    boolean existsByClinicId(Long clinicId);


    Optional<ClinicWhatsAppConfig>
    findByWhatsappNumber(String whatsappNumber);

    boolean existsByWhatsappNumber(String whatsappNumber);
}