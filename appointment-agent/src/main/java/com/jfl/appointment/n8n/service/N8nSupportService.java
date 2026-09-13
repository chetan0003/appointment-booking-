package com.jfl.appointment.n8n.service;


import com.jfl.appointment.entity.ClinicWhatsAppConfig;
import com.jfl.appointment.entity.Patient;
import com.jfl.appointment.entity.PatientQrStatus;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.n8n.dto.ClinicWhatsappConfigDto;
import com.jfl.appointment.n8n.dto.IdentifyPatientResponse;
import com.jfl.appointment.repository.ClinicRepository;
import com.jfl.appointment.repository.ClinicWhatsAppConfigRepository;
import com.jfl.appointment.repository.PatientQrCredentialRepository;
import com.jfl.appointment.repository.PatientRepository;
import com.jfl.appointment.service.PatientQrService;
import com.jfl.appointment.service.PatientQrTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * The two genuinely new pieces the QR/Twilio flow needed - everything else
 * it calls already existed and is just wrapped by N8nApiController.
 */
@Service
@RequiredArgsConstructor
public class N8nSupportService {

    private final ClinicRepository clinicRepository;
    private final PatientRepository patientRepository;
    private final ClinicWhatsAppConfigRepository clinicWhatsAppConfigRepository;
    private final PatientQrTokenService patientQrService;
    private final PatientQrCredentialRepository credentialRepository;

    @Transactional(readOnly = true)
    public ClinicWhatsappConfigDto resolveClinicByWhatsappNumber(String whatsappNumber) {
//        Clinic clinic = clinicRepository.findByWhatsappNumber(whatsappNumber)
//                .orElseThrow(() -> new NotFoundException(
//                        "No clinic registered for WhatsApp number: " + whatsappNumber));

        ClinicWhatsAppConfig whatsAppConfig = clinicWhatsAppConfigRepository.findByWhatsappNumber(whatsappNumber).orElseThrow(() -> new NotFoundException(
                "No clinic registered for WhatsApp number: " + whatsappNumber));

        if (whatsAppConfig == null) {
            // A clinic MUST have a default service configured before the QR
            // flow can work at all - availability/booking both require a
            // serviceId, and this flow never asks the patient for one. Fail
            // loudly here rather than letting a later call silently 400.
            throw new IllegalStateException(
                    "Clinic " + whatsAppConfig.getClinic().getId() + " has no default_service_id configured - "
                            + "required for the QR booking flow, set it during clinic onboarding.");
        }

//        return new ClinicWhatsappConfigDto(
//                whatsAppConfig.getId(),
//                whatsAppConfig.getClinic().getName(),
//                whatsAppConfig.getDefaultService().getId(),
//                whatsAppConfig.getTwilioSubaccountSid(),
//                whatsAppConfig.getTwilioAuthToken(),
//                whatsAppConfig.getTwilioWhatsappSender()
//        );
        return null;
    }

    /**
     * Returns a response with a null patientId for an invalid/expired token,
     * rather than a 404 - the n8n flow's "Handle Identify Result" node checks
     * for that directly, so an invalid QR scan gets a clean, specific reply
     * instead of parsing an error body shape.
     */
    @Transactional(readOnly = true)
    public IdentifyPatientResponse identifyByToken(
            Long clinicId,
            String token
    ) {
        if (token == null || token.isBlank()) {
            return new IdentifyPatientResponse(null, null);
        }

        String tokenHash = patientQrService.hashToken(token);

        return credentialRepository
                .findByTokenHashAndStatus(
                        tokenHash,
                        PatientQrStatus.ACTIVE
                )
                .filter(qr -> qr.getExpiresAt() == null
                        || qr.getExpiresAt().isAfter(LocalDateTime.now()))
                .filter(qr -> qr.getPatient().getClinic().getId().equals(clinicId))
                .map(qr -> {
                    Patient patient = qr.getPatient();

                    return new IdentifyPatientResponse(
                            patient.getId(),
                            patient.getName()
                    );
                })
                .orElse(new IdentifyPatientResponse(null, null));
    }
}
