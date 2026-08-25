package com.jfl.appointment.n8n.service;

import com.jfl.appointment.n8n.dto.SessionResponse;
import com.jfl.appointment.n8n.dto.UpdateSessionRequest;
import com.jfl.appointment.entity.*;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConversationSessionService {

    private final ConversationSessionRepository sessionRepository;
    private final ClinicRepository clinicRepository;
    private final ServiceOfferingRepository serviceRepository;
    private final DoctorRepository doctorRepository;

    /**
     * Called at the top of every n8n run for an inbound WhatsApp message.
     * Finds the patient's in-progress session, or starts a fresh one.
     * This is what lets the AI "remember" what's already been collected
     * (design doc section 8) without n8n itself holding any state.
     */
    @Transactional
    public SessionResponse findOrCreateActiveSession(Long clinicId, String whatsappNumber) {
        ConversationSession session = sessionRepository.findActiveSession(clinicId, whatsappNumber)
                .orElseGet(() -> {
                    Clinic clinic = clinicRepository.findById(clinicId)
                            .orElseThrow(() -> new NotFoundException("Clinic not found: " + clinicId));
                    ConversationSession s = new ConversationSession();
                    s.setClinic(clinic);
                    s.setWhatsappNumber(whatsappNumber);
                    s.setState(ConversationState.STARTED);
                    return sessionRepository.save(s);
                });
        return toResponse(session);
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
                s.getState()
        );
    }
}
