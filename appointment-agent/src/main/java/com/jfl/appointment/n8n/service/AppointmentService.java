package com.jfl.appointment.n8n.service;

import com.jfl.appointment.entity.*;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.exception.SlotUnavailableException;
import com.jfl.appointment.n8n.dto.AppointmentResponse;
import com.jfl.appointment.n8n.dto.CreateAppointmentRequest;
import com.jfl.appointment.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final ServiceOfferingRepository serviceRepository;
    private final ClinicRepository clinicRepository;
    private final AvailabilityService availabilityService;
    private final ConversationSessionService sessionService;

    /**
     * Runs in its own REQUIRES_NEW transaction so the pessimistic lock is
     * held for the shortest possible window and released as soon as this
     * method returns (commit/rollback), rather than for the lifetime of
     * whatever larger transaction (e.g. a conversation-session update)
     * might be calling it.
     *
     * Flow (design doc section 15/16, "check twice" + "prevent double booking"):
     *   1. Pessimistic-lock this doctor's CONFIRMED appointments for the date
     *      -> any concurrent booking attempt for the same doctor/date blocks here.
     *   2. Re-run slot computation (the SECOND, authoritative check).
     *   3. If the requested slot isn't in the fresh list -> reject.
     *   4. Insert. The partial unique index in schema.sql is the final safety
     *      net if this logic is ever bypassed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AppointmentResponse createAppointment(CreateAppointmentRequest request) {
        log.info("createAppointment: {},{},{}",request.clinicId(), request.patientName(),request.appointmentDate());
        Clinic clinic = clinicRepository.findById(request.clinicId())
                .orElseThrow(() -> new NotFoundException("Clinic not found: " + request.clinicId()));

        Doctor doctor = doctorRepository.findById(request.doctorId())
                .orElseThrow(() -> new NotFoundException("Doctor not found: " + request.doctorId()));

        ServiceOffering service = serviceRepository.findById(request.serviceId())
                .orElseThrow(() -> new NotFoundException("Service not found: " + request.serviceId()));

        // Idempotency: if a request with this key already produced a booking, return it
        // instead of creating a duplicate (handles WhatsApp/n8n webhook retries).
        if (request.idempotencyKey() != null) {
            Optional<Appointment> existing = appointmentRepository.findByAppointmentCode(
                    "IDEMP-" + request.idempotencyKey());
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        // Step 1: acquire the lock BEFORE re-checking availability.
        appointmentRepository.lockDoctorAppointmentsForDate(doctor.getId(), request.appointmentDate());

        // Step 2: authoritative re-check, now that we hold the lock.
        List<LocalTime> freshSlots = availabilityService.computeSlots(
                clinic.getId(), doctor, service, request.appointmentDate());

        if (!freshSlots.contains(request.startTime())) {
            throw new SlotUnavailableException(
                    "Requested slot " + request.startTime() + " on " + request.appointmentDate()
                            + " is no longer available for this doctor.");
        }

        Patient patient = patientRepository.findByClinicIdAndWhatsappNumberAndName(
                        clinic.getId(), request.whatsappNumber(),request.patientName())
                .orElseGet(() -> {
                    Patient p = new Patient();
                    p.setClinic(clinic);
                    p.setWhatsappNumber(request.whatsappNumber());
                    p.setName(request.patientName());
                    return patientRepository.save(p);
                });
        // Keep the name fresh in case they gave a fuller name this time.
        patient.setName(request.patientName());

        Appointment appointment = new Appointment();
        appointment.setAppointmentCode(generateAppointmentCode(request.idempotencyKey()));
        appointment.setClinic(clinic);
        appointment.setDoctor(doctor);
        appointment.setService(service);
        appointment.setPatient(patient);
        appointment.setAppointmentDate(request.appointmentDate());
        appointment.setStartTime(request.startTime());
        appointment.setEndTime(request.startTime().plusMinutes(service.getDurationMinutes()));
        appointment.setStatus(AppointmentStatus.CONFIRMED);

        // Step 4: insert. uq_doctor_slot_active (partial unique index) is the
        // final DB-level guarantee even if steps 1-3 were somehow raced.
        Appointment saved = appointmentRepository.save(appointment);

        if (request.sessionId() != null) {
            sessionService.markBooked(request.sessionId());
        }

        return toResponse(saved);
    }

    private String generateAppointmentCode(String idempotencyKey) {
        if (idempotencyKey != null) {
            return "IDEMP-" + idempotencyKey;
        }
        return "APT-" + System.currentTimeMillis() % 1_000_000 + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(
                a.getAppointmentCode(),
                a.getStatus(),
                a.getAppointmentDate(),
                a.getStartTime(),
                a.getEndTime(),
                a.getDoctor().getName(),
                a.getService().getName()
        );
    }
}
