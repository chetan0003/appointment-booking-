package com.jfl.appointment.n8n.service;

import com.jfl.appointment.config.ConfigProperties;
import com.jfl.appointment.exception.DailyBookingLimitException;
import com.jfl.appointment.n8n.dto.SessionResponse;
import com.jfl.appointment.n8n.dto.UpdateSessionRequest;
import com.jfl.appointment.entity.*;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.policy.entity.PolicyCategory;
import com.jfl.appointment.policy.service.ClinicPolicyResolver;
import com.jfl.appointment.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationSessionService {

    private final ConversationSessionRepository sessionRepository;
    private final ClinicRepository clinicRepository;
    private final ServiceOfferingRepository serviceRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final ConfigProperties configProperties;
    private final ClinicPolicyResolver policyResolver;



//    @Transactional
//    public SessionResponse findOrCreateActiveSession(Long clinicId, String whatsappNumber, Long patientId,String source) {
//        // 1. List all terminal states that count as "finished" sessions
//        List<ConversationState> terminalStates = List.of(
//                ConversationState.BOOKED,
//                ConversationState.ABANDONED
//        );
//
//        // 2. Look up the most recent non-terminal session for this phone number
//        Optional<ConversationSession> activeSession = sessionRepository
//                .findTopByWhatsappNumberAndStateNotInOrderByUpdatedAtDesc(whatsappNumber, terminalStates);
//
//        // Clinic QR: clinicId present, patientId null, source=CLINIC_QR (or similar)
//        if ("CLINIC".equalsIgnoreCase(source) && clinicId != null && patientId == null) {
//            activeSession.ifPresent(existing -> {
//                existing.setState(ConversationState.ABANDONED);
//                sessionRepository.save(existing);
//            });
//            Clinic clinic = clinicRepository.findById(clinicId)
//                    .orElseThrow(() -> new NotFoundException("Clinic not found: " + clinicId));
//            ConversationSession newSession = new ConversationSession();
//            newSession.setClinic(clinic);
//            newSession.setPatient(null); // walk-in
//            newSession.setWhatsappNumber(whatsappNumber);
//            newSession.setState(ConversationState.STARTED);
//            return toResponse(sessionRepository.save(newSession));
//        }
//
//
//        // 3. IF QR Code data is provided (new booking flow started)
//        if ("PATIENT".equalsIgnoreCase(source) && patientId != null && clinicId != null) {
//            // Abandon previous incomplete session if user scanned a new QR code
//            activeSession.ifPresent(existing -> {
//                existing.setState(ConversationState.ABANDONED);
//                sessionRepository.save(existing);
//            });
//
//            Clinic clinic = clinicRepository.findById(clinicId)
//                    .orElseThrow(() -> new NotFoundException("Clinic not found: " + clinicId));
//            Patient patient = patientRepository.findById(patientId)
//                    .orElseThrow(() -> new NotFoundException("Patient not found: " + patientId));
//
//            ConversationSession newSession = new ConversationSession();
//            newSession.setClinic(clinic);
//            newSession.setPatient(patient);
//            newSession.setWhatsappNumber(whatsappNumber);
//            newSession.setState(ConversationState.STARTED);
//
//            return toResponse(sessionRepository.save(newSession));
//        }
//
//        // 4. IF plain text reply (patientId & clinicId are null), return ongoing session
//        ConversationSession existing = activeSession
//                .orElseThrow(() -> new NotFoundException("No active session found for number: " + whatsappNumber));
//
//        return toResponse(existing);
//    }

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private String generateSessionCode() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(CODE_ALPHABET.charAt(SECURE_RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }

    private void assignSessionCode(ConversationSession session) {
        session.setSessionCode(generateSessionCode());
        session.setCodeExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
    }

    @Transactional
    public SessionResponse findOrCreateActiveSession(
            Long clinicId,
            String whatsappNumber,
            Long patientId,
            String source) {

        log.info("Get Session for WhatsApp Number : {}",whatsappNumber);
        //policyResolver.getActivePolicy(clinicId, PolicyCategory.APPOINTMENT);
        List<ConversationState> terminalStates = List.of(
                ConversationState.BOOKED,
                ConversationState.ABANDONED
        );

        Optional<ConversationSession> activeSession = sessionRepository
                .findTopByWhatsappNumberAndStateNotInOrderByUpdatedAtDesc(
                        whatsappNumber, terminalStates);

        String src = source == null ? "" : source.trim();
        int sessionTtlMinutes = configProperties.booking().sessionTtlMinutes(); // 30

        // -------------------------------------------------------------------------
        // CLINIC QR
        // -------------------------------------------------------------------------
        if ("CLINIC".equalsIgnoreCase(src) && clinicId != null) {

            assertDailySuccessLimitOrThrow(clinicId, whatsappNumber);

            if (activeSession.isPresent()) {
                ConversationSession existing = activeSession.get();

                if (existing.getClinic() != null
                        && clinicId.equals(existing.getClinic().getId())
                        && existing.getPatient() == null) {

                    // Idle / expired session → abandon and create fresh below
                    if (isSessionExpired(existing, sessionTtlMinutes)) {
                        existing.setState(ConversationState.ABANDONED);
                        existing.setSessionCode(null);
                        existing.setCodeExpiresAt(null);
                        sessionRepository.save(existing);
                    } else {
                        // Reuse: refresh code if missing or expired
                        if (existing.getSessionCode() == null
                                || existing.getCodeExpiresAt() == null
                                || existing.getCodeExpiresAt().isBefore(Instant.now())) {
                            assignSessionCode(existing, sessionTtlMinutes);
                            sessionRepository.save(existing);
                        }
                        return toResponse(existing);
                    }
                } else {
                    existing.setState(ConversationState.ABANDONED);
                    existing.setSessionCode(null);
                    existing.setCodeExpiresAt(null);
                    sessionRepository.save(existing);
                }
            }

            Clinic clinic = clinicRepository.findById(clinicId)
                    .orElseThrow(() -> new NotFoundException("Clinic not found: " + clinicId));

            ConversationSession newSession = new ConversationSession();
            newSession.setClinic(clinic);
            newSession.setPatient(null);
            newSession.setWhatsappNumber(whatsappNumber);
            newSession.setState(ConversationState.STARTED);
            assignSessionCode(newSession, sessionTtlMinutes);
            return toResponse(sessionRepository.save(newSession));
        }

        // -------------------------------------------------------------------------
        // PATIENT QR
        // -------------------------------------------------------------------------
        if ("PATIENT".equalsIgnoreCase(src) && patientId != null && clinicId != null) {

            assertDailySuccessLimitOrThrow(clinicId, whatsappNumber);

            activeSession.ifPresent(existing -> {
                existing.setState(ConversationState.ABANDONED);
                existing.setSessionCode(null);
                existing.setCodeExpiresAt(null);
                sessionRepository.save(existing);
            });

            Clinic clinic = clinicRepository.findById(clinicId)
                    .orElseThrow(() -> new NotFoundException("Clinic not found: " + clinicId));
            Patient patient = patientRepository.findById(patientId)
                    .orElseThrow(() -> new NotFoundException("Patient not found: " + patientId));

            ConversationSession newSession = new ConversationSession();
            newSession.setClinic(clinic);
            newSession.setPatient(patient);
            newSession.setWhatsappNumber(whatsappNumber);
            newSession.setState(ConversationState.STARTED);
            assignSessionCode(newSession, sessionTtlMinutes);
            return toResponse(sessionRepository.save(newSession));
        }

        // -------------------------------------------------------------------------
        // ONGOING (plain WhatsApp reply)
        // -------------------------------------------------------------------------
        ConversationSession existing = activeSession
                .orElseThrow(() -> new NotFoundException(
                        "No active session found for number: " + whatsappNumber));

        if (isSessionExpired(existing, sessionTtlMinutes)) {
            existing.setState(ConversationState.ABANDONED);
            existing.setSessionCode(null);
            existing.setCodeExpiresAt(null);
            sessionRepository.save(existing);
            throw new NotFoundException(
                    "Session expired. Please scan the QR code again.");
        }

        if (existing.getClinic() != null) {
            assertDailySuccessLimitOrThrow(existing.getClinic().getId(), whatsappNumber);
        }

        // Ensure code still present for later createAppointment
        if (existing.getSessionCode() == null
                || existing.getCodeExpiresAt() == null
                || existing.getCodeExpiresAt().isBefore(Instant.now())) {
            assignSessionCode(existing, sessionTtlMinutes);
            sessionRepository.save(existing);
        }

        return toResponse(existing);
    }

    private void assignSessionCode(ConversationSession session, int ttlMinutes) {
        session.setSessionCode(generateSessionCode());
        session.setCodeExpiresAt(Instant.now().plus(ttlMinutes, ChronoUnit.MINUTES));
    }
    /** Idle / TTL: code expiry OR last update older than TTL */
    private boolean isSessionExpired(ConversationSession session, int ttlMinutes) {
        Instant now = Instant.now();
        if (session.getCodeExpiresAt() != null && session.getCodeExpiresAt().isBefore(now)) {
            return true;
        }
        // If you store updatedAt as LocalDateTime on AuditableEntity:
        if (session.getUpdatedAt() != null) {
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(ttlMinutes);
            if (session.getUpdatedAt().isBefore(cutoff)) {
                return true;
            }
        }
        return false;
    }
    @Transactional
    public SessionResponse updateSession(Long sessionId, UpdateSessionRequest request) {
        ConversationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Session not found: " + sessionId));

        if (request.intent() != null) session.setIntent(request.intent());
        if (request.serviceId() != null) {
            session.setService(serviceRepository.findById(request.serviceId())
                    .orElseThrow(() -> new NotFoundException("Service not found: " + request.serviceId())));
        }
        if (request.doctorId() != null) {
            session.setDoctor(doctorRepository.findById(request.doctorId())
                    .orElseThrow(() -> new NotFoundException("Doctor not found: " + request.doctorId())));
        }
        if (request.appointmentDate() != null) session.setAppointmentDate(request.appointmentDate());
        if (request.selectedStartTime() != null) session.setSelectedStartTime(request.selectedStartTime());
        if (request.patientName() != null) session.setPatientName(request.patientName());
        // State transition is explicit and always driven by n8n/AI decision,
        // never inferred implicitly here - keeps the state machine auditable.
        if (request.state() != null) session.setState(request.state());

        return toResponse(session);
    }

    private void assertDailySuccessLimitOrThrow(Long clinicId, String whatsappNumber) {
        if (clinicId == null || whatsappNumber == null || whatsappNumber.isBlank()) {
            return;
        }

        ZoneId zone = ZoneId.of("Asia/Kolkata");
        LocalDate today = LocalDate.now(zone);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        long successToday = appointmentRepository.countSuccessfulCreatedToday(
                whatsappNumber.trim(),
                clinicId,
                startOfDay,
                endOfDay,
                List.of(
                        AppointmentStatus.CONFIRMED,
                        AppointmentStatus.COMPLETED,
                        AppointmentStatus.CANCELLED
                )
        );

        int max = configProperties.booking().maxSuccessPerPhonePerClinicPerDay(); // 3
        if (successToday >= max) {
            throw new DailyBookingLimitException(
                    "Daily booking limit reached for this number. You can book again tomorrow.");
        }
    }

    /**
     * Called right after a successful POST /api/appointments. Marks the
     * session BOOKED so uq_session_active_per_patient releases and the
     * patient can immediately start a new booking conversation if they want.
     */
    @Transactional
    public void markBooked(Long sessionId) {
        ConversationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Session not found: " + sessionId));
        session.setState(ConversationState.BOOKED);
    }

    private SessionResponse toResponse(ConversationSession s) {
        return new SessionResponse(
                s.getId(),
                s.getClinic().getId(),
                s.getWhatsappNumber(),
                s.getIntent(),
                s.getService() != null ? s.getService().getId() : null,
                s.getDoctor() != null ? s.getDoctor().getId() : null,
                s.getAppointmentDate(),
                s.getSelectedStartTime(),
                s.getPatientName(),
                s.getPatient() != null ? s.getPatient().getId() : null,
                s.getState(),
                s.getSessionCode(),
                s.getCodeExpiresAt()
        );
    }
}
