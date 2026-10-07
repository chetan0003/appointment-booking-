package com.jfl.appointment.service;


import com.jfl.appointment.dto.QueueEntryResponse;
import com.jfl.appointment.entity.*;
import com.jfl.appointment.exception.ConflictException;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.repository.AppointmentQrCredentialRepository;
import com.jfl.appointment.repository.AppointmentRepository;
import com.jfl.appointment.repository.ClinicQueueEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClinicQueueService {

    private final AppointmentRepository appointmentRepository;
    private final ClinicQueueEntryRepository queueEntryRepository;
    private final QueueNumberGenerator queueNumberGenerator;
    private final AppointmentQrCredentialRepository qrRepository;
    private final AppointmentQrTokenService tokenService;
    private final AppointmentQrService appointmentQrService;

    @Transactional
    public QueueEntryResponse checkIn(Long appointmentId) {

        Appointment appointment = appointmentRepository
                .findByIdForUpdate(appointmentId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Appointment not found: " + appointmentId
                        )
                );

        // Idempotency: do not generate another token.
        var existing = queueEntryRepository
                .findByAppointment_Id(appointmentId);

        if (existing.isPresent()) {
            ClinicQueueEntry entry = existing.get();

            return toResponse(entry);
        }

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new ConflictException(
                    "Only confirmed appointments can be checked in"
            );
        }

        Clinic clinic = appointment.getClinic();

        ZoneId zoneId = ZoneId.of(clinic.getTimezone());

        LocalDate queueDate = LocalDate.now(zoneId);

        LocalDateTime checkedInAt =
                LocalDateTime.now(zoneId);

        int number = queueNumberGenerator.nextNumber(
                clinic.getId(),
                queueDate
        );

        ClinicQueueEntry entry = new ClinicQueueEntry();

        entry.setClinic(clinic);
        entry.setAppointment(appointment);
        entry.setQueueDate(queueDate);
        entry.setQueueNumber(number);
        entry.setQueueToken("A-" + String.format("%03d", number));
        entry.setQueueStatus(QueueStatus.WAITING);
        entry.setCheckedInAt(checkedInAt);

        appointment.setStatus(AppointmentStatus.CHECKED_IN);

        ClinicQueueEntry saved =
                queueEntryRepository.save(entry);

        return toResponse(saved);
    }


    // ============================================================
    // SCAN QR -> CHECK-IN -> USING QR CODE
    // ============================================================

    @Transactional
    public QueueEntryResponse checkInUsingQr(String rawToken) {

        // --------------------------------------------------------
        // Validate token
        // --------------------------------------------------------

        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException(
                    "QR token cannot be empty"
            );
        }

        // --------------------------------------------------------
        // Hash raw token
        // --------------------------------------------------------

        String tokenHash =
                tokenService.hashToken(rawToken);

        // --------------------------------------------------------
        // Find QR credential
        // --------------------------------------------------------

        AppointmentQrCredential credential =
                qrRepository.findByTokenHash(tokenHash)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Invalid appointment QR"
                                )
                        );

        // --------------------------------------------------------
        // Validate QR
        // ACTIVE + expiry check
        // --------------------------------------------------------

        appointmentQrService.validateQr(credential);

        // --------------------------------------------------------
        // Get appointment
        // --------------------------------------------------------

        Appointment appointment =
                credential.getAppointment();

        if (appointment == null) {
            throw new NotFoundException(
                    "Appointment associated with QR not found"
            );
        }

        // --------------------------------------------------------
        // Check appointment status
        // --------------------------------------------------------

        if (appointment.getStatus()
                == AppointmentStatus.CANCELLED) {

            throw new ConflictException(
                    "Appointment is cancelled"
            );
        }

        if (appointment.getStatus()
                == AppointmentStatus.COMPLETED) {

            throw new ConflictException(
                    "Appointment is already completed"
            );
        }

        if (appointment.getStatus()
                == AppointmentStatus.NO_SHOW) {

            throw new ConflictException(
                    "Appointment is marked as no-show"
            );
        }

        if (appointment.getStatus()
                == AppointmentStatus.IN_CONSULTATION) {

            throw new ConflictException(
                    "Appointment is already in consultation"
            );
        }

        // --------------------------------------------------------
        // If already checked in
        // --------------------------------------------------------

        if (appointment.getStatus()
                == AppointmentStatus.CHECKED_IN) {

            throw new ConflictException(
                    "Appointment is already checked in"
            );
        }

        // --------------------------------------------------------
        // IMPORTANT:
        //
        // Do NOT create QueueEntry here.
        //
        // Existing queueService.checkIn() already handles:
        // - Queue number
        // - Queue token
        // - QueueEntry creation
        // - Queue status WAITING
        // - Appointment CHECKED_IN
        // - Clinic timezone
        // --------------------------------------------------------

        QueueEntryResponse response =
                this.checkIn(
                        appointment.getId()
                );

        // --------------------------------------------------------
        // Mark QR as USED
        // Only after successful check-in
        // --------------------------------------------------------

        credential.setStatus(
                AppointmentQrStatus.USED
        );

        credential.setScannedAt(
                LocalDateTime.now()
        );

        qrRepository.save(credential);

        log.info(
                "Appointment checked in using QR. appointmentId={}, qrCredentialId={}",
                appointment.getId(),
                credential.getId()
        );

        return response;
    }

    private void validateForCheckIn(
            AppointmentQrCredential credential,
            Appointment appointment
    ) {

        // Validate QR first
        appointmentQrService.validateQr(credential);

        // --------------------------------------------------------
        // Appointment status validation
        // --------------------------------------------------------

        if (appointment.getStatus()
                == AppointmentStatus.CANCELLED) {

            throw new IllegalStateException(
                    "Appointment is cancelled"
            );
        }

        if (appointment.getStatus()
                == AppointmentStatus.COMPLETED) {

            throw new IllegalStateException(
                    "Appointment is already completed"
            );
        }

        /*
         * If your AppointmentStatus contains NO_SHOW,
         * keep this validation.
         */

        if (appointment.getStatus()
                == AppointmentStatus.NO_SHOW) {

            throw new IllegalStateException(
                    "Appointment is marked as no-show"
            );
        }

        /*
         * Prevent duplicate check-in.
         */

        if (appointment.getStatus()
                == AppointmentStatus.CHECKED_IN) {

            throw new IllegalStateException(
                    "Appointment is already checked in"
            );
        }

        /*
         * Prevent scanning after consultation has started.
         */

        if (appointment.getStatus()
                == AppointmentStatus.IN_CONSULTATION) {

            throw new IllegalStateException(
                    "Appointment is already in consultation"
            );
        }
    }

    private QueueEntryResponse toResponse(
            ClinicQueueEntry entry) {

        return new QueueEntryResponse(
                entry.getAppointment().getId(),
                entry.getId(),
                entry.getQueueToken(),
                entry.getQueueNumber(),
                entry.getQueueDate(),
                entry.getQueueStatus()
        );
    }
}
