package com.jfl.appointment.dashboard.service;

import com.jfl.appointment.dashboard.dto.*;
import com.jfl.appointment.entity.*;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.exception.SlotUnavailableException;
import com.jfl.appointment.n8n.service.AvailabilityService;
import com.jfl.appointment.repository.*;
import com.jfl.appointment.security.IntegrationUtil;
import com.jfl.appointment.security.SecurityContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentAdminService {

    private final AppointmentRepository appointmentRepository;
    private final ClinicRepository clinicRepository;
    private final ClinicAccessService clinicAccessService;
    private final SecurityContextService securityContextService;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final ServiceOfferingRepository serviceOfferingRepository;
    private final DoctorServiceRepository doctorServiceRepository;
    private final NotificationService notificationService;
    private final AvailabilityService availabilityService;

    @Transactional
    public AppointmentListItemDto createAppointment(
            Long clinicId,
            CreateAppointmentRequest request) {

        log.info(
                "Creating normal appointment. clinicId={}, patientId={}, doctorId={}, serviceId={}",
                clinicId,
                request.patientId(),
                request.doctorId(),
                request.serviceId()
        );

        // Idempotency: if a request with this key already produced a booking, return it
        // instead of creating a duplicate (handles WhatsApp/n8n webhook retries).
        if (request.idempotencyKey() != null) {
            Optional<Appointment> existing = appointmentRepository.findByAppointmentCode(
                    "IDEMP-" + request.idempotencyKey());
            if (existing.isPresent()) {
                return toDto(existing.get());
            }
        }
        // =====================================================
        // 1. Validate clinic
        // =====================================================

        Clinic clinic = clinicRepository
                .findById(clinicId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Clinic not found: " + clinicId
                        )
                );

        // =====================================================
        // 2. Validate patient
        // =====================================================

        Patient patient = patientRepository
                .findById(request.patientId())
                .orElseThrow(() ->
                        new NotFoundException(
                                "Patient not found: "
                                        + request.patientId()
                        )
                );

        if (!patient.getClinic().getId().equals(clinicId)) {
            throw new IllegalArgumentException(
                    "Patient does not belong to this clinic."
            );
        }

        // =====================================================
        // 3. Validate doctor
        // =====================================================

        Doctor doctor = doctorRepository
                .findById(request.doctorId())
                .filter(d ->
                        d.getClinic().getId().equals(clinicId)
                )
                .orElseThrow(() ->
                        new NotFoundException(
                                "Doctor not found: "
                                        + request.doctorId()
                        )
                );

        if (!doctor.isActive()) {
            throw new IllegalArgumentException(
                    "Doctor is not active."
            );
        }

        // =====================================================
        // 4. Validate service
        // =====================================================

        ServiceOffering service =
                serviceOfferingRepository
                        .findByIdAndClinicIdAndActiveTrue(
                                request.serviceId(),
                                clinicId
                        )
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Service not found or does not belong to clinic: "
                                                + request.serviceId()
                                )
                        );

        // =====================================================
        // 5. Validate doctor-service mapping
        // =====================================================

        boolean doctorProvidesService =
                doctorServiceRepository
                        .existsByDoctorIdAndServiceId(
                                doctor.getId(),
                                service.getId()
                        );

        if (!doctorProvidesService) {
            throw new IllegalArgumentException(
                    "Doctor does not provide the selected service."
            );
        }

        //=================check availability ===================
        List<LocalTime> slots = availabilityService.computeSlots(clinicId, doctor, service, request.appointmentDate());
        if (slots.isEmpty())
            throw new NotFoundException("Slot is not available");
        // =====================================================
        // 6. Validate time
        // =====================================================

        if (!request.startTime()
                .isBefore(request.endTime())) {

            throw new IllegalArgumentException(
                    "Start time must be before end time."
            );
        }

        // =====================================================
        // 7. Validate service duration
        // =====================================================

        LocalTime expectedEnd =
                request.startTime()
                        .plusMinutes(
                                service.getDurationMinutes()
                        );

        if (!expectedEnd.equals(request.endTime())) {

            throw new IllegalArgumentException(
                    "Appointment time does not match service duration."
            );
        }

        // =====================================================
        // 8. Check doctor slot conflict
        // =====================================================

        boolean conflict =
                appointmentRepository.existsConflict(
                        doctor.getId(),
                        request.appointmentDate(),
                        request.startTime(),
                        request.endTime()
                );

        if (conflict) {
            throw new SlotUnavailableException(
                    "The selected appointment slot is not available."
            );
        }

        // =====================================================
        // 9. Create appointment
        // =====================================================

        Appointment appointment =
                new Appointment();

        appointment.setClinic(clinic);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setService(service);
        appointment.setAppointmentCode(IntegrationUtil.generateAppointmentCode(request.idempotencyKey()));
        appointment.setAppointmentDate(
                request.appointmentDate()
        );

        appointment.setStartTime(
                request.startTime()
        );

        appointment.setEndTime(
                request.endTime()
        );

        appointment.setStatus(
                AppointmentStatus.CONFIRMED
        );

        // Normal appointment
        appointment.setFollowUpOfAppointment(null);

        Appointment savedAppointment =
                appointmentRepository.save(appointment);

        log.info(
                "Appointment created successfully. appointmentId={}",
                savedAppointment.getId()
        );

        // =====================================================
        // 10. Create booking notification
        // =====================================================

         notificationService.createBookingConfirmation(
                 savedAppointment
         );

        return toDto(savedAppointment);
    }


    @Transactional
    public AppointmentListItemDto createNextAppointment(
            Long previousAppointmentId,
            CreateNextAppointmentRequest request) {

        log.info(
                "Creating next appointment. previousAppointmentId={}",
                previousAppointmentId
        );

        // =====================================================
        // 1. Get previous appointment
        // =====================================================

        Appointment previousAppointment =
                appointmentRepository
                        .findById(previousAppointmentId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Previous appointment not found: "
                                                + previousAppointmentId
                                )
                        );

        // =====================================================
        // 2. Previous appointment must be completed
        // =====================================================

        if (previousAppointment.getStatus()
                != AppointmentStatus.COMPLETED) {

            throw new IllegalArgumentException(
                    "Next appointment can only be scheduled "
                            + "for a completed appointment."
            );
        }

        // =====================================================
        // 3. Validate date/time
        // =====================================================

        if (!request.startTime()
                .isBefore(request.endTime())) {

            throw new IllegalArgumentException(
                    "Start time must be before end time."
            );
        }

        if (!request.appointmentDate()
                .isAfter(
                        previousAppointment.getAppointmentDate()
                )) {

            throw new IllegalArgumentException(
                    "Next appointment date must be after "
                            + "the previous appointment date."
            );
        }

        // =====================================================
        // 4. Get existing doctor/service
        // =====================================================

        Doctor doctor =
                previousAppointment.getDoctor();

        ServiceOffering service =
                previousAppointment.getService();

        Clinic clinic =
                previousAppointment.getClinic();

        Patient patient =
                previousAppointment.getPatient();

        // =====================================================
        // 5. Validate active doctor
        // =====================================================

        if (!doctor.isActive()) {
            throw new IllegalArgumentException(
                    "Doctor is not active."
            );
        }

        // =====================================================
        // 6. Validate service
        // =====================================================

        if (!service.isActive()) {
            throw new IllegalArgumentException(
                    "Service is not active."
            );
        }

        // =====================================================
        // 7. Validate service duration
        // =====================================================

        LocalTime expectedEnd =
                request.startTime()
                        .plusMinutes(
                                service.getDurationMinutes()
                        );

        if (!expectedEnd.equals(request.endTime())) {

            throw new IllegalArgumentException(
                    "Appointment time does not match service duration."
            );
        }

        // =====================================================
        // 8. Check slot conflict
        // =====================================================

        boolean conflict =
                appointmentRepository.existsConflict(
                        doctor.getId(),
                        request.appointmentDate(),
                        request.startTime(),
                        request.endTime()
                );

        if (conflict) {

            throw new SlotUnavailableException(
                    "The selected appointment slot is not available."
            );
        }

        // =====================================================
        // 9. Create NEW appointment
        // =====================================================

        Appointment nextAppointment =
                new Appointment();

        nextAppointment.setClinic(clinic);
        nextAppointment.setPatient(patient);
        nextAppointment.setDoctor(doctor);
        nextAppointment.setService(service);
        nextAppointment.setAppointmentCode(IntegrationUtil.generateAppointmentCode(null));
        nextAppointment.setAppointmentDate(
                request.appointmentDate()
        );

        nextAppointment.setStartTime(
                request.startTime()
        );

        nextAppointment.setEndTime(
                request.endTime()
        );

        nextAppointment.setStatus(
                AppointmentStatus.CONFIRMED
        );

        // IMPORTANT:
        // Link new appointment to previous appointment

        nextAppointment.setFollowUpOfAppointment(
                previousAppointment
        );

        // =====================================================
        // 10. Save
        // =====================================================

        Appointment savedAppointment =
                appointmentRepository.save(
                        nextAppointment
                );

        log.info(
                "Next appointment created. previousAppointmentId={}, newAppointmentId={}",
                previousAppointmentId,
                savedAppointment.getId()
        );

        // =====================================================
        // 11. Booking notification
        // =====================================================

        // notificationService.createBookingConfirmation(
        //         savedAppointment
        // );

        return toDto(savedAppointment);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentListItemDto> listAppointments(Long clinicId, LocalDate from, LocalDate to,
                                                         Long doctorId, AppointmentStatus status,
                                                         Long serviceId, Pageable pageable) {

        if (!clinicAccessService
                .hasAccessToClinic(clinicId)) {

            throw new SecurityException(
                    "No access to clinic"
            );
        }

        if (securityContextService.hasRole("DOCTOR")) {

            ClinicUser clinicUser =
                    clinicAccessService
                            .getClinicUser(clinicId);

            doctorId =
                    clinicUser.getDoctor().getId();
        }

        return appointmentRepository
                .findForDashboard(
                        clinicId,
                        serviceId,
                        doctorId,
                        status,
                        pageable
                )
                .map(this::toDto);
    }


    /**
     * Cancelling just flips the status. Nothing else to do at the DB level -
     * uq_doctor_slot_active is a PARTIAL unique index (WHERE status = 'CONFIRMED'),
     * so the moment this row stops being CONFIRMED its slot is automatically free
     * for a new booking. No separate "release the slot" step needed.
     */
    @Transactional
    public AppointmentListItemDto cancelAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found: " + appointmentId));

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new SlotUnavailableException(
                    "Only a CONFIRMED appointment can be cancelled (current status: " + appointment.getStatus() + ")");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        return toDto(appointment);
    }

    @Transactional
    public AppointmentListItemDto rescheduleAppointment(
            Long appointmentId,
            RescheduleAppointmentRequest request) {

        Appointment appointment =
                appointmentRepository.findById(appointmentId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Appointment not found: " + appointmentId
                                )
                        );

        // ---------------------------------------------
        // Validate date/time
        // ---------------------------------------------

        if (!request.startTime().isBefore(request.endTime())) {
            throw new IllegalArgumentException(
                    "Start time must be before end time."
            );
        }

        // ---------------------------------------------
        // Don't allow rescheduling completed/cancelled
        // appointments
        // ---------------------------------------------

        if (appointment.getStatus() == AppointmentStatus.COMPLETED
                || appointment.getStatus() == AppointmentStatus.CANCELLED
                || appointment.getStatus() == AppointmentStatus.NO_SHOW) {

            throw new IllegalArgumentException(
                    "Appointment cannot be rescheduled from status: "
                            + appointment.getStatus()
            );
        }

        // ---------------------------------------------
        // Check slot availability
        // ---------------------------------------------

        boolean slotAvailable =
                appointmentRepository.existsConflictForReschedule(
                        appointment.getDoctor().getId(),
                        request.appointmentDate(),
                        request.startTime(),
                        request.endTime(),
                        appointmentId
                );

        if (slotAvailable) {
            throw new SlotUnavailableException(
                    "The selected slot is no longer available."
            );
        }

        // ---------------------------------------------
        // Save old values for notification/audit
        // ---------------------------------------------

        LocalDate oldDate =
                appointment.getAppointmentDate();

        LocalTime oldStart =
                appointment.getStartTime();

        LocalTime oldEnd =
                appointment.getEndTime();

        // ---------------------------------------------
        // Update appointment
        // ---------------------------------------------

        appointment.setAppointmentDate(
                request.appointmentDate()
        );

        appointment.setStartTime(
                request.startTime()
        );

        appointment.setEndTime(
                request.endTime()
        );

        Appointment savedAppointment =
                appointmentRepository.save(appointment);

        // ---------------------------------------------
        // Notification
        // ---------------------------------------------

        // Create RESCHEDULED notification here.
        //
        // notificationService.createRescheduledNotification(
        //         savedAppointment,
        //         oldDate,
        //         oldStart,
        //         oldEnd
        // );

        return toDto(savedAppointment);
    }

    private AppointmentListItemDto toDto(Appointment a) {
        return new AppointmentListItemDto(
                a.getId(),
                a.getAppointmentCode(),
                a.getAppointmentDate(),
                a.getStartTime(),
                a.getEndTime(),
                a.getStatus(),
                a.getDoctor().getId(),
                a.getDoctor().getName(),
                a.getService().getId(),
                a.getService().getName(),
                a.getPatient().getName(),
                a.getPatient().getWhatsappNumber(),
                a.getFollowUpOfAppointment() != null ? a.getFollowUpOfAppointment().getId() : null,
                a.getSuggestedFollowUpDate()
        );
    }

    public void validateStatusTransition(
            AppointmentStatus current,
            AppointmentStatus next) {

        boolean valid = switch (current) {

            case CONFIRMED -> next == AppointmentStatus.CHECKED_IN
                    || next == AppointmentStatus.CANCELLED
                    || next == AppointmentStatus.NO_SHOW;

            case CHECKED_IN -> next == AppointmentStatus.WAITING
                    || next == AppointmentStatus.CANCELLED;

            case WAITING -> next == AppointmentStatus.IN_CONSULTATION;

            case IN_CONSULTATION -> next == AppointmentStatus.COMPLETED;

            case COMPLETED, CANCELLED, NO_SHOW -> false;
        };

        if (!valid) {
            throw new IllegalStateException(
                    "Invalid appointment status transition: "
                            + current + " -> " + next
            );
        }
    }

    @Transactional
    public AppointmentListItemDto suggestFollowUp(
            Long appointmentId,
            FollowUpRequest request) {

        Appointment appointment =
                appointmentRepository.findById(appointmentId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Appointment not found: "
                                                + appointmentId
                                )
                        );

        // ---------------------------------------------
        // Follow-up should normally be added after
        // consultation is completed
        // ---------------------------------------------

        if (appointment.getStatus()
                != AppointmentStatus.IN_CONSULTATION) {

            throw new IllegalArgumentException(
                    "Follow-up can only be suggested for a completed appointment."
            );
        }

        // ---------------------------------------------
        // Validate follow-up date
        // ---------------------------------------------

        if (request.suggestedFollowUpDate()
                .isBefore(appointment.getAppointmentDate())) {

            throw new IllegalArgumentException(
                    "Follow-up date cannot be before the appointment date."
            );
        }

        // ---------------------------------------------
        // Save suggested follow-up date
        // ---------------------------------------------

        appointment.setSuggestedFollowUpDate(
                request.suggestedFollowUpDate()
        );
        //added by me
        appointment.setStatus(AppointmentStatus.COMPLETED);
        Appointment savedAppointment =
                appointmentRepository.save(appointment);

        // ---------------------------------------------
        // Optional notification
        // ---------------------------------------------

//         notificationService.createFollowUpSuggestedNotification(
//                 savedAppointment
//         );

        return toDto(savedAppointment);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentListItemDto> getPatientAppointments(
            Long clinicId,
            Long patientId,
            int page,
            int size) {

        patientRepository.findByIdAndClinicId(patientId, clinicId)
                .orElseThrow(() ->
                        new NotFoundException("Patient not found for this clinic."));

        Pageable pageable = PageRequest.of(
                page,
                size
        );

        Page<Appointment> appointments =
                appointmentRepository.findByClinicIdAndPatientIdOrderByAppointmentDateAscStartTimeAsc(
                        clinicId,
                        patientId,
                        pageable
                );

        return appointments.map(this::toDto);
    }

}
