package com.jfl.appointment.service;

import com.jfl.appointment.dto.PatientQrResponse;
import com.jfl.appointment.dto.WhatsAppQrResolveResponse;
import com.jfl.appointment.entity.*;

import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.repository.ClinicRepository;
import com.jfl.appointment.repository.ClinicWhatsAppConfigRepository;
import com.jfl.appointment.repository.PatientQrCredentialRepository;
import com.jfl.appointment.repository.PatientRepository;
import com.jfl.appointment.util.AESUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PatientQrService {

    private final PatientRepository patientRepository;
    private final ClinicRepository clinicRepository;
    private final PatientQrCredentialRepository qrRepository;
    private final ClinicWhatsAppConfigRepository clinicWhatsAppConfigRepository;
    private final PatientQrTokenService tokenService;
    private final WhatsAppQrLinkService whatsAppQrLinkService;
    private final QrCodeGeneratorService qrCodeGeneratorService;
    private final AESUtil aesUtil;


    @Transactional
    public PatientQrResponse generateQr(
            Long clinicId,
            Long patientId
    ) {

        // ============================================================
        // 1. VALIDATE CLINIC
        // ============================================================

        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Clinic not found with id: " + clinicId
                        )
                );


        // ============================================================
        // 2. VALIDATE PATIENT
        // ============================================================

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Patient not found with id: " + patientId
                        )
                );


        // ============================================================
        // 3. PATIENT MUST BELONG TO THIS CLINIC
        // ============================================================

        if (patient.getClinic() == null ||
                !patient.getClinic().getId().equals(clinicId)) {

            throw new IllegalArgumentException(
                    "Patient does not belong to this clinic"
            );
        }


        // ============================================================
        // 4. GET CLINIC WHATSAPP CONFIG
        // ============================================================

        ClinicWhatsAppConfig whatsappConfig =
                clinicWhatsAppConfigRepository
                        .findByClinicId(clinicId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "WhatsApp is not configured for this clinic"
                                )
                        );


        // ============================================================
        // 5. CHECK WHATSAPP STATUS
        // ============================================================

        if (whatsappConfig.getStatus()
                != WhatsAppConfigStatus.ACTIVE) {

            throw new IllegalStateException(
                    "WhatsApp is not active for this clinic"
            );
        }


        // ============================================================
        // 6. CHECK EXISTING ACTIVE QR
        // ============================================================

        Optional<PatientQrCredential> existingQrOptional =
                qrRepository
                        .findFirstByPatientIdAndStatusOrderByCreatedAtDesc(
                                patientId,
                                PatientQrStatus.ACTIVE
                        );


        if (existingQrOptional.isPresent()) {

            PatientQrCredential existingQr =
                    existingQrOptional.get();


            // ========================================================
            // 7. CHECK WHETHER EXISTING QR IS EXPIRED
            // ========================================================

            boolean expired =
                    existingQr.getExpiresAt() != null
                            && existingQr.getExpiresAt()
                            .isBefore(LocalDateTime.now());


            // ========================================================
            // 8. EXISTING QR IS VALID
            // ========================================================

            if (!expired) {

                /*
                 * IMPORTANT:
                 *
                 * If encryptedToken is available,
                 * we can reconstruct the same WhatsApp URL
                 * and return the same QR.
                 */
                if (existingQr.getEncryptedToken() != null
                        && !existingQr.getEncryptedToken().isBlank()) {

                    String rawToken =
                            decryptToken(
                                    existingQr.getEncryptedToken()
                            );


                    String whatsappUrl =
                            whatsAppQrLinkService.generateLink(
                                    whatsappConfig.getWhatsappNumber(),
                                    rawToken
                            );


                    byte[] qrImage =
                            qrCodeGeneratorService.generateQrCode(
                                    whatsappUrl,
                                    500,
                                    500
                            );


                    String qrImageBase64 =
                            Base64.getEncoder()
                                    .encodeToString(qrImage);


                    // Return SAME QR
                    return new PatientQrResponse(
                            patient.getId(),
                            clinic.getId(),
                            existingQr.getId(),
                            existingQr.getStatus().name(),
                            whatsappConfig.getWhatsappNumber(),
                            whatsappUrl,
                            qrImageBase64,
                            existingQr.getCreatedAt(),
                            existingQr.getExpiresAt()
                    );
                }


                /*
                 * ====================================================
                 * LEGACY QR
                 * ====================================================
                 *
                 * Existing QR has no encrypted token.
                 *
                 * We cannot reconstruct the original QR token,
                 * therefore mark it expired and generate a new QR.
                 */

                existingQr.setStatus(
                        PatientQrStatus.EXPIRED
                );

                qrRepository.save(existingQr);
            }


            // ========================================================
            // 9. EXISTING QR IS EXPIRED
            // ========================================================

            if (expired) {

                existingQr.setStatus(
                        PatientQrStatus.EXPIRED
                );

                qrRepository.save(existingQr);
            }
        }


        // ============================================================
        // 10. GENERATE NEW SECURE TOKEN
        // ============================================================

        String rawToken =
                tokenService.generateToken();


        // ============================================================
        // 11. HASH TOKEN
        // ============================================================

        String tokenHash =
                tokenService.hashToken(rawToken);


        // ============================================================
        // 12. CREATE NEW QR CREDENTIAL
        // ============================================================

        PatientQrCredential credential =
                new PatientQrCredential();

        credential.setPatient(patient);

        credential.setTokenHash(tokenHash);

        /*
         * Store encrypted token so that next time
         * we can return the SAME QR.
         */
        credential.setEncryptedToken(
                encryptToken(rawToken)
        );

        credential.setStatus(
                PatientQrStatus.ACTIVE
        );

        credential.setCreatedAt(
                LocalDateTime.now()
        );


        /*
         * QR validity.
         *
         * If you want permanent QR later,
         * we can remove this expiry.
         */
        credential.setExpiresAt(
                LocalDateTime.now().plusYears(1)
        );


        // ============================================================
        // 13. SAVE QR CREDENTIAL
        // ============================================================

        PatientQrCredential savedCredential =
                qrRepository.save(credential);


        // ============================================================
        // 14. CREATE WHATSAPP DEEP LINK
        // ============================================================

        String whatsappUrl =
                whatsAppQrLinkService.generateLink(
                        whatsappConfig.getWhatsappNumber(),
                        rawToken
                );


        // ============================================================
        // 15. GENERATE QR IMAGE
        // ============================================================

        byte[] qrImage =
                qrCodeGeneratorService.generateQrCode(
                        whatsappUrl,
                        500,
                        500
                );


        // ============================================================
        // 16. CONVERT IMAGE TO BASE64
        // ============================================================

        String qrImageBase64 =
                Base64.getEncoder()
                        .encodeToString(qrImage);


        // ============================================================
        // 17. RETURN RESPONSE
        // ============================================================

        return new PatientQrResponse(
                patient.getId(),
                clinic.getId(),
                savedCredential.getId(),
                savedCredential.getStatus().name(),
                whatsappConfig.getWhatsappNumber(),
                whatsappUrl,
                qrImageBase64,
                savedCredential.getCreatedAt(),
                savedCredential.getExpiresAt()
        );
    }


    @Transactional
    public void revokeQr(
            Long clinicId,
            Long patientId
    ) {

        Patient patient =
                patientRepository.findById(patientId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Patient not found with id: "
                                                + patientId
                                )
                        );


        if (!patient.getClinic().getId().equals(clinicId)) {

            throw new IllegalArgumentException(
                    "Patient does not belong to this clinic"
            );
        }


        qrRepository
                .findFirstByPatientIdAndStatusOrderByCreatedAtDesc(
                        patientId,
                        PatientQrStatus.ACTIVE
                )
                .ifPresentOrElse(

                        qr -> {

                            qr.setStatus(
                                    PatientQrStatus.REVOKED
                            );

                            qr.setRevokedAt(
                                    LocalDateTime.now()
                            );

                            qrRepository.save(qr);
                        },

                        () -> {
                            throw new NotFoundException(
                                    "Active QR not found for patient"
                            );
                        }
                );
    }


    @Transactional(readOnly = true)
    public WhatsAppQrResolveResponse resolveQr(
            String rawToken
    ) {

        if (rawToken == null ||
                rawToken.isBlank()) {

            return new WhatsAppQrResolveResponse(
                    false,
                    null,
                    null,
                    null,
                    "QR token is required"
            );
        }


        // Hash received token
        String tokenHash =
                tokenService.hashToken(rawToken);


        // Find active QR
        PatientQrCredential credential =
                qrRepository
                        .findByTokenHashAndStatus(
                                tokenHash,
                                PatientQrStatus.ACTIVE
                        )
                        .orElse(null);


        if (credential == null) {

            return new WhatsAppQrResolveResponse(
                    false,
                    null,
                    null,
                    null,
                    "Invalid or revoked QR"
            );
        }


        // Check expiry
        if (credential.getExpiresAt() != null &&
                credential.getExpiresAt()
                        .isBefore(LocalDateTime.now())) {

            return new WhatsAppQrResolveResponse(
                    false,
                    null,
                    null,
                    null,
                    "QR has expired"
            );
        }


        Patient patient =
                credential.getPatient();


        Clinic clinic =
                patient.getClinic();


        return new WhatsAppQrResolveResponse(
                true,
                patient.getId(),
                clinic.getId(),
                patient.getName(),
                "Patient identified successfully"
        );
    }

    private String encryptToken(String token) {
        return aesUtil.encrypt(token);
    }

    private String decryptToken(String encryptedToken) {

        if (encryptedToken == null || encryptedToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Encrypted QR token is missing"
            );
        }

        return aesUtil.decrypt(encryptedToken);
    }
}