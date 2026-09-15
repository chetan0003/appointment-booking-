package com.jfl.appointment.n8n.service;

import com.jfl.appointment.n8n.dto.SessionResponse;
import com.jfl.appointment.n8n.dto.UpdateSessionRequest;
import com.jfl.appointment.entity.*;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConversationSessionService {

    private final ConversationSessionRepository sessionRepository;
    private final ClinicRepository clinicRepository;
    private final ServiceOfferingRepository serviceRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    /**
     * Called at the top of every n8n run for an inbound WhatsApp message.
     * Finds the patient's in-progress session, or starts a fresh one.
     * This is what lets the AI "remember" what's already been collected
     * (design doc section 8) without n8n itself holding any state.
     */
//    @Transactional
//    public SessionResponse findOrCreateActiveSession(Long clinicId, String whatsappNumber, Long patientId) {
//        ConversationSession session = sessionRepository.findActiveSession(clinicId, patientId, whatsappNumber)
//                .orElseGet(() -> {
//                    Clinic clinic = clinicRepository.findById(clinicId)
//                            .orElseThrow(() -> new NotFoundException("Clinic not found: " + clinicId));
//                    Patient patient = patientRepository.findById(patientId)
//                            .orElseThrow(() -> new NotFoundException("Clinic not found: " + clinicId));
//                    ConversationSession s = new ConversationSession();
//                    s.setClinic(clinic);
//                    s.setPatient(patient);
//                    s.setWhatsappNumber(whatsappNumber);
//                    s.setState(ConversationState.STARTED);
//                    return sessionRepository.save(s);
//                });
//        return toResponse(session);
//    }

    @Transactional
    public SessionResponse findOrCreateActiveSession(Long clinicId, String whatsappNumber, Long patientId,String source) {
        // 1. List all terminal states that count as "finished" sessions
        List<ConversationState> terminalStates = List.of(
                ConversationState.BOOKED,
                ConversationState.ABANDONED
        );

        // 2. Look up the most recent non-terminal session for this phone number
        Optional<ConversationSession> activeSession = sessionRepository
                .findTopByWhatsappNumberAndStateNotInOrderByUpdatedAtDesc(whatsappNumber, terminalStates);

        // Clinic QR: clinicId present, patientId null, source=CLINIC_QR (or similar)
        if ("CLINIC".equals(source) && clinicId != null && patientId == null) {
            activeSession.ifPresent(existing -> {
                existing.setState(ConversationState.ABANDONED);
                sessionRepository.save(existing);
            });
            Clinic clinic = clinicRepository.findById(clinicId)
                    .orElseThrow(() -> new NotFoundException("Clinic not found: " + clinicId));
            ConversationSession newSession = new ConversationSession();
            newSession.setClinic(clinic);
            newSession.setPatient(null); // walk-in
            newSession.setWhatsappNumber(whatsappNumber);
            newSession.setState(ConversationState.STARTED);
            return toResponse(sessionRepository.save(newSession));
        }


        // 3. IF QR Code data is provided (new booking flow started)
        if ("PATIENT".equalsIgnoreCase(source) && patientId != null && clinicId != null) {
            // Abandon previous incomplete session if user scanned a new QR code
            activeSession.ifPresent(existing -> {
                existing.setState(ConversationState.ABANDONED);
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

            return toResponse(sessionRepository.save(newSession));
        }

        // 4. IF plain text reply (patientId & clinicId are null), return ongoing session
        ConversationSession existing = activeSession
                .orElseThrow(() -> new NotFoundException("No active session found for number: " + whatsappNumber));

        return toResponse(existing);
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
                s.getState()
        );
    }
}
