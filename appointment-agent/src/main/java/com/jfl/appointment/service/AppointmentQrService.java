package com.jfl.appointment.service;

import com.jfl.appointment.config.ConfigProperties;
import com.jfl.appointment.dto.AppointmentQrCredentialResult;
import com.jfl.appointment.dto.AppointmentQrDetailsResponse;
import com.jfl.appointment.entity.Appointment;
import com.jfl.appointment.entity.AppointmentQrCredential;
import com.jfl.appointment.entity.AppointmentQrStatus;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.repository.AppointmentQrCredentialRepository;
import com.jfl.appointment.util.AESUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentQrService {

    private final QrCodeGeneratorService qrCodeGeneratorService;
    private final AppointmentQrCredentialRepository qrRepository;
    private final AppointmentQrTokenService tokenService;
    private final AESUtil aesUtil;
    private final ConfigProperties configProperties;


    // ============================================================
    // CREATE QR CREDENTIAL
    // ============================================================

    @Transactional
    public AppointmentQrCredentialResult createQrCredential(
            Appointment appointment
    ) {

        Optional<AppointmentQrCredential> existing =
                qrRepository.findByAppointmentId(
                        appointment.getId()
                );

        if (existing.isPresent()) {

            AppointmentQrCredential credential =
                    existing.get();

            if (credential.getStatus()
                    == AppointmentQrStatus.ACTIVE) {

                /*
                 * Existing active credential ke case mein
                 * encrypted token ko decrypt karke raw token
                 * return kar sakte hain.
                 */
                String rawToken =
                        aesUtil.decrypt(
                                credential.getEncryptedToken()
                        );

                return new AppointmentQrCredentialResult(
                        credential.getId(),
                        rawToken,
                        credential.getExpiresAt()
                );
            }
        }

        // ------------------------------------------------
        // Generate new raw token
        // ------------------------------------------------

        String rawToken =
                tokenService.generateToken();

        // ------------------------------------------------
        // Hash token for DB lookup
        // ------------------------------------------------

        String tokenHash =
                tokenService.hashToken(rawToken);

        // ------------------------------------------------
        // Create credential
        // ------------------------------------------------

        AppointmentQrCredential credential =
                new AppointmentQrCredential();

        credential.setAppointment(appointment);

        credential.setTokenHash(tokenHash);

        credential.setEncryptedToken(
                aesUtil.encrypt(rawToken)
        );

        credential.setStatus(
                AppointmentQrStatus.ACTIVE
        );

        credential.setCreatedAt(
                LocalDateTime.now()
        );

        credential.setExpiresAt(
                calculateExpiry(appointment)
        );

        AppointmentQrCredential saved =
                qrRepository.save(credential);

        // ------------------------------------------------
        // Return raw token to caller
        // ------------------------------------------------

        return new AppointmentQrCredentialResult(
                saved.getId(),
                rawToken,
                saved.getExpiresAt()
        );
    }


    // ============================================================
    // GENERATE QR IMAGE
    // ============================================================

    @Transactional(readOnly = true)
    public byte[] generateQrImage(String token) {

        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                    "QR token cannot be empty"
            );
        }

        AppointmentQrCredential credential =
                qrRepository.findByTokenHash(
                        tokenService.hashToken(token)
                ).orElseThrow(() ->
                        new NotFoundException(
                                "Appointment QR not found"
                        )
                );

        // Validate QR
        validateQr(credential);

        /*
         * QR should open the frontend page,
         * NOT directly expose the dashboard API.
         *
         * Example:
         *
         * https://app.holamd.app/check-in?token=xxxxx
         */

        String payload =
                configProperties.app()
                        .holaMdAppBaseUrl()
                        + "/check-in?token="
                        + token;

        log.info(
                "Generating appointment QR. appointmentId={}",
                credential.getAppointment().getId()
        );

        return qrCodeGeneratorService.generateQrCode(
                payload,
                500,
                500
        );
    }



    // ============================================================
    // VALIDATE QR
    // ============================================================

    public void validateQr(
            AppointmentQrCredential credential
    ) {

        if (credential == null) {
            throw new NotFoundException(
                    "Appointment QR not found"
            );
        }

        // --------------------------------------------------------
        // Status validation
        // --------------------------------------------------------

        if (credential.getStatus()
                != AppointmentQrStatus.ACTIVE) {

            if (credential.getStatus()
                    == AppointmentQrStatus.USED) {

                throw new IllegalStateException(
                        "Appointment QR has already been used"
                );
            }

            if (credential.getStatus()
                    == AppointmentQrStatus.EXPIRED) {

                throw new IllegalStateException(
                        "Appointment QR has expired"
                );
            }

            if (credential.getStatus()
                    == AppointmentQrStatus.REVOKED) {

                throw new IllegalStateException(
                        "Appointment QR has been revoked"
                );
            }

            throw new IllegalStateException(
                    "Appointment QR is not active"
            );
        }

        // --------------------------------------------------------
        // Expiry validation
        // --------------------------------------------------------

        if (credential.getExpiresAt() != null
                && !credential.getExpiresAt()
                .isAfter(LocalDateTime.now())) {

            credential.setStatus(
                    AppointmentQrStatus.EXPIRED
            );

            qrRepository.save(credential);

            throw new IllegalStateException(
                    "Appointment QR has expired"
            );
        }
    }


    // ============================================================
    // GET APPOINTMENT BY TOKEN
    // ============================================================
    @Transactional(readOnly = true)
    public AppointmentQrDetailsResponse getAppointmentDetailsByToken(
            String rawToken
    ) {

        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException(
                    "QR token cannot be empty"
            );
        }

        // Hash the token received from QR
        String tokenHash =
                tokenService.hashToken(rawToken);

        // Find QR credential
        AppointmentQrCredential credential =
                qrRepository.findByTokenHash(tokenHash)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Invalid appointment QR"
                                )
                        );

        // Validate QR
        validateQr(credential);

        Appointment appointment =
                credential.getAppointment();

        if (appointment == null) {
            throw new NotFoundException(
                    "Appointment associated with QR not found"
            );
        }

        return new AppointmentQrDetailsResponse(
                appointment.getId(),
                appointment.getAppointmentCode(),
                appointment.getPatient().getId(),
                appointment.getPatient().getName(),
                appointment.getPatient().getProfileStatus(),
                appointment.getDoctor().getName(),
                appointment.getClinic().getName(),
                appointment.getService().getName(),
                appointment.getAppointmentDate(),
                appointment.getStartTime(),
                appointment.getStatus(),
                credential.getStatus(),
                credential.getExpiresAt()
        );
    }


    // ============================================================
    // CALCULATE QR EXPIRY
    // ============================================================

    private LocalDateTime calculateExpiry(
            Appointment appointment
    ) {

        if (appointment.getAppointmentDate() == null
                || appointment.getStartTime() == null) {

            throw new IllegalArgumentException(
                    "Appointment date and start time are required"
            );
        }

        LocalDateTime appointmentDateTime =
                LocalDateTime.of(
                        appointment.getAppointmentDate(),
                        appointment.getStartTime()
                );

        /*
         * QR remains valid for 2 hours
         * from appointment start time.
         */

        return appointmentDateTime.plusHours(2);
    }


    // ============================================================
    // ENCRYPT TOKEN
    // ============================================================

    private String encryptToken(String token) {

        return aesUtil.encrypt(token);
    }
}