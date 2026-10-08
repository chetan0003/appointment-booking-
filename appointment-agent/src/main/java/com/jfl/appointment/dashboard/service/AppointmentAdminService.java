package com.jfl.appointment.dashboard.service;

import com.jfl.appointment.config.ConfigProperties;
import com.jfl.appointment.dashboard.dto.*;
import com.jfl.appointment.dto.AppointmentQrCredentialResult;
import com.jfl.appointment.entity.*;
import com.jfl.appointment.exception.*;
import com.jfl.appointment.n8n.service.AvailabilityService;
import com.jfl.appointment.n8n.service.ConversationSessionService;
import com.jfl.appointment.policy.service.AppointmentPolicyService;
import com.jfl.appointment.repository.*;
import com.jfl.appointment.security.IntegrationUtil;
import com.jfl.appointment.security.SecurityContextService;
import com.jfl.appointment.service.AppointmentQrService;
import com.jfl.appointment.service.SubscriptionFeatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    private final AvailabilityService availabilityService;
    private final NotificationSchedulingService notificationSchedulingService;
    private final NotificationRepository notificationRepository;
    private final AppNotificationRepository appNotificationRepository;
    private final SubscriptionFeatureService subscriptionFeatureService;
    private final AppointmentPaymentRepository appointmentPaymentRepository;
    private final NotificationService notificationService;
    private final ConversationSessionRepository sessionRepository;
    private final BookingAttemptRepository bookingAttemptRepository;
    private final ConversationSessionService sessionService;
    private final ConfigProperties configProperties;
    private final ClinicQueueEntryRepository queueEntryRepository;
    private final AppointmentPolicyService appointmentPolicyService;
    private final AppointmentQrService appointmentQrService;
    private final AppointmentQrCredentialRepository appointmentQrCredentialRepository;

    @Transactional
    public AppointmentListItemDto createAppointment(
            Long clinicId,
            CreateAppointmentRequest request) {

        final boolean isWhatsApp = isWhatsAppRequest(request);
        final Long clinicId2 = securityContextService.getClinicId();

        log.info(
                "createAppointment source={}, clinicId={}, patientId={}, patientName={}, doctorId={}, date={} IdenpotentKey={}",
                isWhatsApp ? "WHATSAPP" : "DASHBOARD",
                clinicId != null ? clinicId : request.clinicId(),
                request.patientId(),
                request.patientName(),
                request.doctorId(),
                request.appointmentDate(),
                request.idempotencyKey()
        );
        if (!Objects.equals(clinicId, clinicId2)) {
            throw new SecurityException("User does not have access to clinic: " + clinicId);
        }

        Long resolvedClinicId = clinicId != null ? clinicId : request.clinicId();
        if (resolvedClinicId == null) {
            throw new IllegalArgumentException("clinicId is required.");
        }

        // -------------------------------------------------------------------------
        // 0. Subscription
        // -------------------------------------------------------------------------
        ClinicSubscription subscription =
                subscriptionFeatureService.getActiveSubscription(resolvedClinicId);

        if (isWhatsApp) {
            subscriptionFeatureService.validateFeatureOnWhats(resolvedClinicId, subscription);
        } else {
            subscriptionFeatureService.validateFeature(
                    resolvedClinicId, subscription, SubscriptionFeature.APPOINTMENTS);
        }

        // -------------------------------------------------------------------------
        // 1. Idempotency (both channels) — FIRST
        // -------------------------------------------------------------------------
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            Optional<Appointment> existing = appointmentRepository.findByAppointmentCode(
                    "IDEMP-" + request.idempotencyKey());
            if (existing.isPresent()) {
                log.info("Idempotent hit key={}", request.idempotencyKey());
                return toDto(existing.get(), null, null);
            }
        }

        // -------------------------------------------------------------------------
        // 2. Clinic / doctor / service
        // -------------------------------------------------------------------------
        Clinic clinic = clinicRepository.findById(resolvedClinicId)
                .orElseThrow(() -> new NotFoundException("Clinic not found: " + resolvedClinicId));

        Doctor doctor = doctorRepository.findById(request.doctorId())
                .filter(d -> d.getClinic().getId().equals(resolvedClinicId))
                .orElseThrow(() -> new NotFoundException("Doctor not found: " + request.doctorId()));

        if (!doctor.isActive()) {
            throw new IllegalArgumentException("Doctor is not active.");
        }

        ServiceOffering service = serviceOfferingRepository
                .findByIdAndClinicIdAndActiveTrue(request.serviceId(), resolvedClinicId)
                .orElseThrow(() -> new NotFoundException(
                        "Service not found or inactive: " + request.serviceId()));

        if (!doctorServiceRepository.existsByDoctorIdAndServiceId(doctor.getId(), service.getId())) {
            throw new IllegalArgumentException("Doctor does not provide the selected service.");
        }

        // -------------------------------------------------------------------------
        // 3. WhatsApp-only: session code + spam limits
        // -------------------------------------------------------------------------
        ConversationSession session = null;
        String whatsappNumber = null;

        if (isWhatsApp) {
            if (request.qrType() == null || request.qrType().isBlank()) {
                throw new IllegalArgumentException("qrType is required for WhatsApp booking.");
            }

            whatsappNumber = firstNonBlank(request.whatsappNumber(), null);
            session = validateWhatsAppSession(request, resolvedClinicId);
            if (whatsappNumber == null && session != null) {
                whatsappNumber = session.getWhatsappNumber();
            }
            if (whatsappNumber == null || whatsappNumber.isBlank()) {
                throw new IllegalArgumentException("whatsappNumber is required for WhatsApp booking.");
            }

            // Record attempt + rate limit (5 / 10 min / phone+clinic)
            recordBookingAttempt(resolvedClinicId, whatsappNumber, request.idempotencyKey());
            assertAttemptRateLimit(resolvedClinicId, whatsappNumber);

            // Daily success cap (3 / day / phone+clinic) — session-proof
            assertDailySuccessLimit(resolvedClinicId, whatsappNumber);

            // Same phone + same slot duplicate
            assertNoDuplicatePhoneSlot(
                    resolvedClinicId,
                    whatsappNumber,
                    request.appointmentDate(),
                    request.startTime()
            );
        }

        // -------------------------------------------------------------------------
        // 4. Lock doctor day + re-check slot (race-safe, both channels)
        // -------------------------------------------------------------------------
        appointmentRepository.lockDoctorAppointmentsForDate(doctor.getId(), request.appointmentDate());

        List<LocalTime> freshSlots = availabilityService.computeSlots(
                clinic.getId(), doctor, service, request.appointmentDate());

        if (!freshSlots.contains(request.startTime())) {
            throw new SlotUnavailableException(
                    "Requested slot " + request.startTime() + " on " + request.appointmentDate()
                            + " is no longer available for this doctor.");
        }

        LocalTime endTime = request.startTime().plusMinutes(service.getDurationMinutes());
        if (request.endTime() != null && !request.endTime().equals(endTime)) {
            throw new IllegalArgumentException("Appointment time does not match service duration.");
        }

        boolean conflict = appointmentRepository.existsConflict(
                doctor.getId(),
                request.appointmentDate(),
                request.startTime(),
                endTime
        );
        if (conflict) {
            throw new SlotUnavailableException("The selected appointment slot is not available.");
        }

        // -------------------------------------------------------------------------
        // 5. Resolve patient
        // -------------------------------------------------------------------------
        Patient patient = resolvePatient(request, clinic, isWhatsApp);

        //--------------------------------------------------------------------------
        // 5.1 Appointment Policy Validation
        // -------------------------------------------------------------------------

//        LocalDateTime appointmentDateTime =
//                request.appointmentDate().atTime(request.startTime());
//
//        int existingAppointmentsToday =
//                appointmentRepository.countPatientAppointmentsForDate(
//                        patient.getId(),
//                        clinic.getId(),
//                        request.appointmentDate(),
//                        List.of(
//                                AppointmentStatus.CONFIRMED
//                        )
//                );
//
//        appointmentPolicyService.validateCreateBooking(
//                resolvedClinicId,
//                appointmentDateTime,
//                existingAppointmentsToday
//        );

        // -------------------------------------------------------------------------
        // 6. Create appointment
        // -------------------------------------------------------------------------
        Appointment appointment = new Appointment();
        appointment.setAppointmentCode(
                IntegrationUtil.generateAppointmentCode(request.idempotencyKey()));
        appointment.setAmount(service.getPrice());
        appointment.setClinic(clinic);
        appointment.setDoctor(doctor);
        appointment.setService(service);
        appointment.setPatient(patient);
        appointment.setAppointmentDate(request.appointmentDate());
        appointment.setStartTime(request.startTime());
        appointment.setEndTime(endTime);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setSource(isWhatsApp ? PatientSource.WHATSAPP : PatientSource.DASHBOARD);
        appointment.setFollowUpOfAppointment(null);
        appointment.setPaymentStatus(AppointmentPaymentStatus.UNPAID);
        if (whatsappNumber != null) {
            appointment.setWhatsappNumber(whatsappNumber);
        }

        Appointment saved = appointmentRepository.save(appointment);
        // -------------------------------------------------------------------------
        // 7. Create Appointment token
        // -------------------------------------------------------------------------
        String rawToken = null;
        if (isWhatsApp) {
            AppointmentQrCredentialResult qrCredential = appointmentQrService.createQrCredential(saved);
            rawToken = qrCredential.token();
        }

        // -------------------------------------------------------------------------
        // 7. Payment
        // -------------------------------------------------------------------------
        AppointmentPayment payment = new AppointmentPayment();
        payment.setAppointment(saved);
        payment.setTotalAmount(service.getPrice());
        payment.setPaidAmount(BigDecimal.ZERO);
        payment.setStatus(AppointmentPaymentStatus.UNPAID);
        appointmentPaymentRepository.save(payment);

        // -------------------------------------------------------------------------
        // 8. Close WhatsApp session (1 booking / session + invalidate code)
        // -------------------------------------------------------------------------
        if (session != null) {
            session.setState(ConversationState.BOOKED);
            session.setSessionCode(null);
            session.setCodeExpiresAt(null);
            sessionRepository.save(session);
        } else if (request.sessionId() != null) {
            sessionService.markBooked(request.sessionId());
        }

        // -------------------------------------------------------------------------
        // 9. Notifications
        // -------------------------------------------------------------------------
        if (subscriptionFeatureService.isWhatsAppNotificationEnable(subscription.getPlan())) {
            if (isWhatsApp) {
                notificationSchedulingService.scheduleBookingReminder(saved);
                // optional: notificationSchedulingService.bookingNotification(saved);
            } else {
                notificationSchedulingService.bookingNotification(saved);
                notificationSchedulingService.scheduleBookingReminder(saved);
            }
        }
        notificationService.createAppointmentNotifications(
                clinic,
                saved,
                doctor.getId(),
                patient.getName(),
                doctor.getName(),
                isWhatsApp
        );

        log.info("Appointment created id={} source={}", saved.getId(), appointment.getSource());
        return toDto(saved, null, rawToken);
    }

    private boolean isWhatsAppRequest(CreateAppointmentRequest request) {
        if (request.sessionId() != null) return true;
        if (request.qrType() != null && !request.qrType().isBlank()) return true;
        if (request.source() != null && "WHATSAPP".equalsIgnoreCase(request.source())) return true;
        return false;
    }

    private ConversationSession validateWhatsAppSession(
            CreateAppointmentRequest request,
            Long clinicId) {

        if (request.sessionId() == null) {
            // Allow without session only if you explicitly want; recommended: require for WA
            throw new IllegalArgumentException("sessionId is required for WhatsApp booking.");
        }

        ConversationSession session = sessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new NotFoundException("Session not found: " + request.sessionId()));

        if (session.getClinic() == null || !session.getClinic().getId().equals(clinicId)) {
            throw new IllegalArgumentException("Session does not belong to this clinic.");
        }
        if (session.getState() == ConversationState.BOOKED
                || session.getState() == ConversationState.ABANDONED) {
            throw new IllegalStateException("Session is no longer active.");
        }
        if (request.sessionCode() == null || request.sessionCode().isBlank()) {
            throw new ForbiddenException("sessionCode is required.");
        }
        if (session.getSessionCode() == null
                || !session.getSessionCode().equalsIgnoreCase(request.sessionCode().trim())) {
            throw new ForbiddenException("Invalid session code.");
        }
        if (session.getCodeExpiresAt() != null
                && session.getCodeExpiresAt().isBefore(Instant.now())) {
            throw new ForbiddenException("Session expired. Please scan the QR again.");
        }
        return session;
    }

    private Patient resolvePatient(
            CreateAppointmentRequest request,
            Clinic clinic,
            boolean isWhatsApp) {

        if (isWhatsApp) {
            // PATIENT QR: prefer existing patientId
            // CLINIC QR: patientId may be null → create from name (+ phone find-or-create later)
            if (request.patientId() != null) {
                return patientRepository.findById(request.patientId())
                        .map(p -> {
                            if (!p.getClinic().getId().equals(clinic.getId())) {
                                throw new IllegalArgumentException("Patient does not belong to this clinic.");
                            }
                            return p;
                        })
                        .orElseGet(() -> createWhatsAppPatient(request, clinic));
            }
            if (request.patientName() == null || request.patientName().isBlank()) {
                throw new IllegalArgumentException("patientName is required when patientId is null.");
            }
            return createWhatsAppPatient(request, clinic);
        }

        // Dashboard: patient must exist
        if (request.patientId() == null) {
            throw new IllegalArgumentException("patientId is required for dashboard booking.");
        }
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new NotFoundException("Patient not found: " + request.patientId()));
        if (!patient.getClinic().getId().equals(clinic.getId())) {
            throw new IllegalArgumentException("Patient does not belong to this clinic.");
        }
        return patient;
    }

    private void assertAttemptRateLimit(Long clinicId, String whatsappNumber) {
        Instant since = Instant.now().minus(10, ChronoUnit.MINUTES);
        long attempts = bookingAttemptRepository
                .countByWhatsappNumberAndClinicIdAndCreatedAtAfter(whatsappNumber, clinicId, since);
        if (attempts > configProperties.booking().maxAttemptsPerPhonePer10Min()) { // 5
            throw new TooManyRequestsException(
                    "Too many booking attempts. Please try again after a few minutes.");
        }
    }

    private void assertDailySuccessLimit(Long clinicId, String whatsappNumber) {
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        LocalDate today = LocalDate.now(zone);

        LocalDateTime startOfDay = today.atStartOfDay();                 // 00:00:00
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();       // next day 00:00:00

        long successToday = appointmentRepository.countSuccessfulCreatedToday(
                whatsappNumber,
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
                    "Daily booking limit reached for this number at this clinic.");
        }
    }

    private void assertNoDuplicatePhoneSlot(
            Long clinicId,
            String whatsappNumber,
            LocalDate date,
            LocalTime startTime) {

        boolean exists = appointmentRepository
                .existsByWhatsappNumberAndClinic_IdAndAppointmentDateAndStartTimeAndStatusIn(
                        whatsappNumber,
                        clinicId,
                        date,
                        startTime,
                        List.of(AppointmentStatus.CONFIRMED)
                );

        if (exists) {
            throw new SlotUnavailableException(
                    "An appointment already exists for this number at the same time.");
        }
    }

    private void recordBookingAttempt(
            Long clinicId,
            String whatsappNumber,
            String idempotencyKey) {

        if (whatsappNumber == null || whatsappNumber.isBlank()) {
            return;
        }

        BookingAttempt attempt = new BookingAttempt();
        attempt.setClinicId(clinicId);
        attempt.setWhatsappNumber(whatsappNumber.trim());
        attempt.setIdempotencyKey(
                idempotencyKey != null && !idempotencyKey.isBlank()
                        ? idempotencyKey.trim()
                        : null);
        attempt.setCreatedAt(Instant.now());

        bookingAttemptRepository.save(attempt);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a.trim();
        if (b != null && !b.isBlank()) return b.trim();
        return null;
    }

    private Patient createWhatsAppPatient(
            com.jfl.appointment.dashboard.dto.CreateAppointmentRequest request,
            Clinic clinic) {

        Patient patient = new Patient();

        patient.setClinic(clinic);
        patient.setName(request.patientName());
        patient.setWhatsappNumber(request.whatsappNumber());
        patient.setSource(PatientSource.WHATSAPP);
        patient.setProfileStatus(PatientProfileStatus.INCOMPLETE);

        return patientRepository.save(patient);
    }

    @Transactional
    public AppointmentListItemDto createNextAppointment(
            Long previousAppointmentId,
            CreateNextAppointmentRequest request) {

        log.info(
                "Creating next appointment. previousAppointmentId={}",
                previousAppointmentId
        );
        //clinic access check
        Long clinicId = securityContextService.getClinicId();
        if(!Objects.equals(request.clinicId(), clinicId)) {
            throw new SecurityException(
                    "User does not have access to clinic: " + clinicId);
        }
        ClinicSubscription subscription =
                subscriptionFeatureService.getActiveSubscription(request.clinicId());
        //VALIDATE SUBSCRIPTION PLAN
        subscriptionFeatureService.validateFeature(
                request.clinicId(),
                subscription,
                SubscriptionFeature.APPOINTMENTS
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
        nextAppointment.setAmount(service.getPrice());
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
        nextAppointment.setSource(PatientSource.DASHBOARD);
        // IMPORTANT:
        // Link new appointment to previous appointment

        nextAppointment.setFollowUpOfAppointment(
                previousAppointment
        );

        // =====================================================
        // 10. Save
        // =====================================================
        nextAppointment.setPaymentStatus(AppointmentPaymentStatus.UNPAID);
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
        // 10. Create Appointment Payment
        // =====================================================

        AppointmentPayment payment = new AppointmentPayment();

        payment.setAppointment(nextAppointment);
        payment.setTotalAmount(service.getPrice());
        payment.setPaidAmount(BigDecimal.ZERO);
        payment.setStatus(AppointmentPaymentStatus.UNPAID);
        appointmentPaymentRepository.save(payment);

        // =====================================================
        // 11. Booking notification
        // =====================================================
        if (subscriptionFeatureService.isWhatsAppNotificationEnable(subscription.getPlan())) {
            Optional<Notification> notification =
                    notificationRepository.findByAppointmentIdAndTypeAndChannel(
                            previousAppointment.getId(), NotificationType.REMINDER_24H, NotificationChannel.WHATSAPP);
            notification.ifPresent(p -> {
                p.setStatus(NotificationStatus.SENT);
            });
            notificationSchedulingService.bookingNotification(savedAppointment);
        }

        // =================================================================================
        // 11. IN-APP notification
        // =================================================================================
        notificationService.createAppointmentNotifications(
                clinic, nextAppointment,
                doctor.getId(),
                patient.getName(),
                doctor.getName(),
                false
        );

        return toDto(savedAppointment, null, null);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentListItemDto> listAppointments(Long clinicId, Long appointmentId, LocalDate from, LocalDate to,
                                                         Long doctorId, AppointmentStatus status,
                                                         Long serviceId, Pageable pageable) {

         clinicId = securityContextService.getClinicId();


        if (securityContextService.hasRole("DOCTOR")) {

            ClinicUser clinicUser =
                    clinicAccessService
                            .getClinicUser(clinicId);

            doctorId =
                    clinicUser.getDoctor().getId();
        }

//        if (from == null) {
//            from = LocalDate.now().withDayOfMonth(1);
//        }
//
//        if (to == null) {
//            to = LocalDate.now()
//                    .withDayOfMonth(LocalDate.now().lengthOfMonth());
//        }

        Specification<Appointment> specification =
                AppointmentSpecification.forDashboard(
                        clinicId,
                        appointmentId,
                        from,
                        to,
                        doctorId,
                        serviceId,
                        status
                );

        return appointmentRepository
                .findAll(specification, pageable)
                .map(m -> this.toDto(m, queueEntryRepository.findByAppointment_Id(m.getId())
                        .orElse(null), null));
    }


    /**
     * Cancelling just flips the status. Nothing else to do at the DB level -
     * uq_doctor_slot_active is a PARTIAL unique index (WHERE status = 'CONFIRMED'),
     * so the moment this row stops being CONFIRMED its slot is automatically free
     * for a new booking. No separate "release the slot" step needed.
     */
    @Transactional
    public AppointmentListItemDto cancelAppointment(Long appointmentId) {
        log.info("cancelAppointment request for  appointmentId:: {}", appointmentId);

        Long clinicId = securityContextService.getClinicId();

        Appointment appointment = appointmentRepository.findByIdAndClinicId(appointmentId,clinicId)
                .orElseThrow(() -> new NotFoundException("Appointment not found: " + appointmentId));

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new SlotUnavailableException(
                    "Only a CONFIRMED appointment can be cancelled (current status: " + appointment.getStatus() + ")");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        ClinicSubscription subscription = subscriptionFeatureService.getActiveSubscription(appointment.getClinic().getId());
        if (subscriptionFeatureService.isWhatsAppNotificationEnable(subscription.getPlan())) {
            notificationSchedulingService.cancelBookingNotification(appointment);
        }
        // =================================================================================
        // 11. IN-APP notification
        // =================================================================================
        notificationService.cancelledAppointmentNotifications(
                appointment.getClinic(), appointment,
                appointment.getDoctor().getId(),
                appointment.getPatient().getName(),
                appointment.getDoctor().getName()
        );
        return toDto(appointment, null, null);
    }

    @Transactional
    public AppointmentListItemDto rescheduleAppointment(
            Long appointmentId,
            RescheduleAppointmentRequest request) {

        log.info("rescheduleAppointment request for appointmentId:: {}", appointmentId);

        Long clinicId = securityContextService.getClinicId();


        Appointment appointment =
                appointmentRepository.findByIdAndClinicId(appointmentId,clinicId)
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
                        clinicId,
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

        // ================================================================================
        // Notification
        // ================================================================================
        ClinicSubscription subscription = subscriptionFeatureService.getActiveSubscription(appointment.getClinic().getId());
        if (subscriptionFeatureService.isWhatsAppNotificationEnable(subscription.getPlan())) {
            notificationSchedulingService.rescheduleBookingReminder(savedAppointment);
            notificationSchedulingService.scheduleRescheduledNotice(savedAppointment);
        }
        // =================================================================================
        // 11. IN-APP notification
        // =================================================================================
        notificationService.rescheduleAppointmentNotifications(
                appointment.getClinic(), appointment,
                appointment.getDoctor().getId(),
                appointment.getPatient().getName(),
                appointment.getDoctor().getName()
        );
        return toDto(savedAppointment, null, null);
    }

    private AppointmentListItemDto toDto(Appointment a, ClinicQueueEntry queueEntry, String rewToken) {
        return new AppointmentListItemDto(
                a.getId(),
                a.getAppointmentCode(),
                a.getAppointmentDate(),
                a.getStartTime(),
                a.getEndTime(),
                a.getAmount(),
                a.getPaymentStatus(),
                a.getStatus(),
                a.getSource() != null ? a.getSource().name() : null,
                a.getDoctor().getId(),
                a.getClinic().getName(),
                a.getDoctor().getName(),
                a.getService().getId(),
                a.getService().getName(),
                a.getPatient().getName(),
                a.getPatient().getWhatsappNumber(),
                a.getFollowUpOfAppointment() != null ? a.getFollowUpOfAppointment().getId() : null,
                a.getSuggestedFollowUpDate(),
                queueEntry != null ? queueEntry.getId() : null,
                queueEntry != null ? queueEntry.getQueueToken() : null,
                queueEntry != null ? queueEntry.getQueueNumber() : null,
                queueEntry != null ? queueEntry.getQueueDate() : null,
                queueEntry != null ? queueEntry.getQueueStatus() : null,
                rewToken

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
        log.info("rescheduleAppointment request for appointmentId:: {}", appointmentId);

        Long clinicId = securityContextService.getClinicId();

        Appointment appointment = appointmentRepository.findByIdAndClinicId(appointmentId,clinicId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Appointment not found: " + appointmentId
                        )
                );

        if (appointment.getStatus() != AppointmentStatus.IN_CONSULTATION) {
            throw new IllegalArgumentException(
                    "Follow-up can only be suggested during consultation."
            );
        }

        if (request.suggestedFollowUpDate() == null) {
            throw new IllegalArgumentException(
                    "Follow-up date is required."
            );
        }

        if (request.suggestedFollowUpDate()
                .isBefore(appointment.getAppointmentDate())) {

            throw new IllegalArgumentException(
                    "Follow-up date cannot be before the appointment date."
            );
        }

        // Save follow-up date
        appointment.setSuggestedFollowUpDate(
                request.suggestedFollowUpDate()
        );

        // Complete appointment
        appointment.setStatus(AppointmentStatus.COMPLETED);

        Appointment savedAppointment =
                appointmentRepository.save(appointment);

        // Update queue entry
        final ClinicQueueEntry[] save = {null};
        queueEntryRepository.findByAppointment_Id(appointmentId)
                .ifPresent(queueEntry -> {
                    queueEntry.setQueueStatus(QueueStatus.COMPLETED);
                    save[0] = queueEntryRepository.save(queueEntry);
                });

        // Optional WhatsApp follow-up notification
        ClinicSubscription subscription =
                subscriptionFeatureService.getActiveSubscription(
                        appointment.getClinic().getId()
                );

        if (subscriptionFeatureService.isWhatsAppNotificationEnable(
                subscription.getPlan())) {

            notificationSchedulingService.scheduleFollowUpSuggestion(
                    savedAppointment,
                    request.suggestedFollowUpDate()
            );
        }

        return toDto(savedAppointment, save[0], null);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentListItemDto> getPatientAppointments(
            Long clinicId,
            Long patientId,
            int page,
            int size) {
        log.info("rescheduleAppointment request for patientId:: {}", patientId);
        RoleName currentUserRole = securityContextService.getCurrentRole();
        clinicId = securityContextService.getClinicId();

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page must be greater than or equal to 0."
            );
        }

        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100."
            );
        }

        patientRepository.findByIdAndClinicId(patientId, clinicId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Patient not found for this clinic."
                        )
                );

        Pageable pageable = PageRequest.of(page, size);

        Page<Appointment> appointments;

        switch (currentUserRole) {

            case CLINIC_ADMIN, STAFF -> appointments =
                    appointmentRepository
                            .findByClinicIdAndPatientIdOrderByAppointmentDateAscStartTimeAsc(
                                    clinicId,
                                    patientId,
                                    pageable
                            );

            case DOCTOR -> {

                ClinicUser clinicUser =
                        clinicAccessService
                                .getClinicUser(clinicId);

                Long doctorId =
                        clinicUser.getDoctor().getId();

                appointments =
                        appointmentRepository.findPatientAppointmentsByDoctor(
                                clinicId,
                                patientId,
                                doctorId,
                                pageable
                        );
            }

            default -> throw new SecurityException(
                    "You are not authorized to view patient appointments."
            );
        }

        return appointments.map(o -> this.toDto(o, null, null));
    }

    @Transactional(readOnly = true)
    public List<WeeklyAppointmentDto> getAppointmentsThisWeek(
            Long clinicId) {
        log.info("getAppointmentsThisWeek request for clinicId {}", clinicId);

        clinicId = securityContextService.getClinicId();

        LocalDate today = LocalDate.now();

        LocalDate startOfWeek =
                today.with(
                        TemporalAdjusters.previousOrSame(
                                DayOfWeek.MONDAY
                        )
                );

        LocalDate endOfWeek =
                startOfWeek.plusDays(6);

        List<Object[]> results =
                appointmentRepository.countAppointmentsByDate(
                        clinicId,
                        startOfWeek,
                        endOfWeek
                );

        Map<LocalDate, Long> appointmentCountMap =
                results.stream()
                        .collect(Collectors.toMap(
                                row -> (LocalDate) row[0],
                                row -> (Long) row[1]
                        ));

        return IntStream.range(0, 7)
                .mapToObj(i -> {

                    LocalDate date =
                            startOfWeek.plusDays(i);

                    return new WeeklyAppointmentDto(
                            date.getDayOfWeek()
                                    .getDisplayName(
                                            TextStyle.SHORT,
                                            Locale.ENGLISH
                                    )
                                    .toUpperCase(),

                            date,

                            appointmentCountMap.getOrDefault(
                                    date,
                                    0L
                            )
                    );
                })
                .toList();
    }

    @Transactional
    @Modifying
    public void deleteAppointment(
            Long clinicId,
            Long appointmentId) {
        log.info("deleteAppointment method started ");


        Appointment appointment = appointmentRepository.findByIdAndClinicId(appointmentId,clinicId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Appointment not found: " + appointmentId
                        )
                );

        notificationRepository.deleteAll(notificationRepository.findByAppointmentIdOrderByCreatedAtDesc(appointmentId));
        appNotificationRepository.deleteAll(appNotificationRepository.findByAppointmentIdAndClinicId(appointmentId, clinicId));
        appointmentPaymentRepository.findByAppointmentId(appointmentId).ifPresent(appointmentPaymentRepository::delete);
        queueEntryRepository.findByAppointment_Id(appointmentId).ifPresent(queueEntryRepository::delete);
        appointmentQrCredentialRepository.findByAppointmentId(appointmentId).ifPresent(appointmentQrCredentialRepository::delete);

        appointmentRepository.delete(appointment);
        log.info("Appointment deleted successfully");
    }
}
