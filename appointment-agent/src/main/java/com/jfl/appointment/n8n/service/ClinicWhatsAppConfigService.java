package com.jfl.appointment.n8n.service;

import com.jfl.appointment.entity.Clinic;
import com.jfl.appointment.entity.ClinicWhatsAppConfig;
import com.jfl.appointment.entity.WhatsAppConfigStatus;
import com.jfl.appointment.entity.WhatsAppProvider;
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
                        new NotFoundException(
                                "Clinic not found with id: " + clinicId
                        )
                );

        // One WhatsApp configuration per clinic
        if (whatsappConfigRepository.existsByClinicId(clinicId)) {
            throw new IllegalArgumentException(
                    "WhatsApp configuration already exists for this clinic."
            );
        }

        // One WhatsApp number can belong to only one clinic
        if (whatsappConfigRepository
                .existsByWhatsappNumber(request.whatsappNumber())) {

            throw new IllegalArgumentException(
                    "This WhatsApp number is already configured " +
                            "for another clinic."
            );
        }

        ClinicWhatsAppConfig config =
                new ClinicWhatsAppConfig();

        config.setClinic(clinic);

        // Clinic WhatsApp number
        config.setWhatsappNumber(
                request.whatsappNumber()
        );

        // WhatsApp provider
        config.setProvider(
                WhatsAppProvider.TWILIO
        );

        // Twilio configuration
        config.setTwilioAccountSid(
                request.twilioAccountSid()
        );

        config.setTwilioSubaccountSid(
                request.twilioSubaccountSid()
        );

        config.setTwilioWhatsappSenderSid(
                request.twilioWhatsappSenderSid()
        );

        // WhatsApp Business Account ID
        config.setWabaId(
                request.wabaId()
        );

        config.setStatus(
                WhatsAppConfigStatus.ACTIVE
        );

        ClinicWhatsAppConfig saved =
                whatsappConfigRepository.save(config);

        return toResponse(saved);
    }

    private WhatsAppConfigResponse toResponse(
            ClinicWhatsAppConfig config) {

        return new WhatsAppConfigResponse(
                config.getId(),
                config.getClinic().getId(),
                config.getWhatsappNumber(),
                config.getProvider(),
                config.getTwilioAccountSid(),
                config.getTwilioSubaccountSid(),
                config.getTwilioWhatsappSenderSid(),
                config.getWabaId(),
                config.getStatus()
        );
    }
}
