package com.jfl.appointment.n8n.service;

import com.jfl.appointment.entity.Clinic;
import com.jfl.appointment.entity.ClinicWhatsAppConfig;
import com.jfl.appointment.entity.WhatsAppConfigStatus;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.n8n.dto.CreateWhatsAppConfigRequest;
import com.jfl.appointment.n8n.dto.WhatsAppConfigResponse;
import com.jfl.appointment.repository.ClinicRepository;
import com.jfl.appointment.repository.ClinicWhatsAppConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClinicWhatsAppConfigService {

    private final ClinicRepository clinicRepository;
    private final ClinicWhatsAppConfigRepository whatsappConfigRepository;

    @Transactional
    public WhatsAppConfigResponse createConfig(
            Long clinicId,
            CreateWhatsAppConfigRequest request) {

        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() ->
                        new NotFoundException("Clinic not found with id: " + clinicId)
                );

        // One WhatsApp configuration per clinic
        if (whatsappConfigRepository.existsByClinicId(clinicId)) {
            throw new IllegalArgumentException(
                    "WhatsApp configuration already exists for this clinic."
            );
        }

        // One phone number can belong to only one clinic in Hola MD
        if (whatsappConfigRepository.existsByPhoneNumberId(request.phoneNumberId())) {
            throw new IllegalArgumentException(
                    "This WhatsApp phone number is already configured for another clinic."
            );
        }

        ClinicWhatsAppConfig config = new ClinicWhatsAppConfig();

        config.setClinic(clinic);
        config.setPhoneNumberId(request.phoneNumberId());
        config.setWabaId(request.wabaId());
        config.setBusinessAccountId(request.businessAccountId());
        config.setDisplayPhoneNumber(request.displayPhoneNumber());

        // Store securely in production
        config.setAccessToken(request.accessToken());

        config.setStatus(WhatsAppConfigStatus.ACTIVE);

        ClinicWhatsAppConfig saved =
                whatsappConfigRepository.save(config);

        return toResponse(saved);
    }

    private WhatsAppConfigResponse toResponse(
            ClinicWhatsAppConfig config) {

        return new WhatsAppConfigResponse(
                config.getId(),
                config.getClinic().getId(),
                config.getPhoneNumberId(),
                config.getWabaId(),
                config.getBusinessAccountId(),
                config.getDisplayPhoneNumber(),
                config.getStatus()
        );
    }
}
