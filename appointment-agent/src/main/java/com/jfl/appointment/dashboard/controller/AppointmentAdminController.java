package com.jfl.appointment.dashboard.controller;

import com.jfl.appointment.dashboard.dto.*;
import com.jfl.appointment.dashboard.service.AppointmentAdminService;
import com.jfl.appointment.entity.*;
import com.jfl.appointment.exception.ConflictException;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.repository.AppointmentRepository;
import com.jfl.appointment.repository.ClinicQueueEntryRepository;
import com.jfl.appointment.repository.NotificationRepository;
import com.jfl.appointment.security.SecurityContextService;
import com.jfl.appointment.service.ClinicContextResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AppointmentAdminController {

    private final AppointmentAdminService appointmentAdminService;
    private final AppointmentRepository appointmentRepository;
    private final NotificationRepository notificationRepository;
    private final ClinicQueueEntryRepository queueEntryRepository;
    private final ClinicContextResolver clinicContextResolver;

    @PreAuthorize("""
            hasAnyRole(
                'SUPER_ADMIN',
                'CLINIC_ADMIN',
                'STAFF',
                'DOCTOR'
            )
            """)
    @PostMapping("/api/dashboard/clinics/appointments")
    public ResponseEntity<ApiResponse<AppointmentListItemDto>> createAppointment(
            @RequestHeader(
                    value = "X-Clinic-Id",
                    required = false
            )
            Long requestedClinicId,
            @Valid @RequestBody CreateAppointmentRequest request) {

        Long clinicId = clinicContextResolver.resolveClinicId(requestedClinicId);
        log.info(
                "Creating appointment. clinicId={}, patientId={}, doctorId={}, serviceId={}, date={}",
                clinicId,
                request.patientId(),
                request.doctorId(),
                request.serviceId(),
                request.appointmentDate()
        );
        CreateAppointmentRequest createAppointmentRequest = request.withSource(PatientSource.DASHBOARD.name());
        AppointmentListItemDto response =
                appointmentAdminService.createAppointment(
                        clinicId,
                        createAppointmentRequest
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Appointment created successfully.",
                                response
                        )
                );
    }


    @PreAuthorize("""
            hasAnyRole(
                'SUPER_ADMIN',
                'CLINIC_ADMIN',
                'STAFF',
                'DOCTOR'
            )
            """)
    @PostMapping("/api/dashboard/appointments/{appointmentId}/next")
    public ResponseEntity<ApiResponse<AppointmentListItemDto>> createNextAppointment(
            @RequestHeader(
                    value = "X-Clinic-Id",
                    required = false
            )
            Long requestedClinicId,
            @PathVariable Long appointmentId,
            @Valid @RequestBody CreateNextAppointmentRequest request) {

        log.info(
                "Creating next appointment. previousAppointmentId={}, date={}, startTime={}",
                appointmentId,
                request.appointmentDate(),
                request.startTime()
        );

        AppointmentListItemDto response =
                appointmentAdminService.createNextAppointment(
                        requestedClinicId,
                        appointmentId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Next appointment scheduled successfully.",
                                response
                        )
                );
    }

    // Dashboard's main list view. `from`/`to` default to "today only" if omitted,
    // so a plain GET with no params gives staff today's schedule at a glance.
    @PreAuthorize("""
            hasAnyRole(
                'SUPER_ADMIN',
                'CLINIC_ADMIN',
                'STAFF',
                'DOCTOR'
            )
            """)
    @GetMapping("/api/dashboard/clinics/appointments")
    public ResponseEntity<ApiResponse<Page<AppointmentListItemDto>>> listAppointments(
            @RequestHeader(
                    value = "X-Clinic-Id",
                    required = false
            )
            Long requestedClinicId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from, @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to, @RequestParam(required = false)
            Long doctorId, @RequestParam(required = false)
            Long serviceId, @RequestParam(required = false)
            Long appointmentId, @RequestParam(required = false)
            AppointmentStatus status,
            @PageableDefault(size = 5, sort = "appointmentDate", direction = Sort.Direction.ASC)
            Pageable pageable) {

//        LocalDate resolvedFrom =
//                from != null
//                        ? from
//                        : LocalDate.now();
//
//        LocalDate resolvedTo =
//                to != null
//                        ? to
//                        : resolvedFrom;

        Page<AppointmentListItemDto> appointments =
                appointmentAdminService.listAppointments(
                        requestedClinicId,
                        appointmentId,
                        from,
                        to,
                        doctorId,
                        status,
                        serviceId,
                        pageable
                );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        ApiResponse.success(
                                "Appointments fetched successfully.",
                                appointments
                        )
                );
    }

    @Transactional
    @PreAuthorize("""
            hasAnyRole(
                'SUPER_ADMIN',
                'CLINIC_ADMIN',
                'STAFF',
                'DOCTOR'
            )
            """)
    @PatchMapping("/api/dashboard/appointments/{appointmentId}/status")
    public ResponseEntity<ApiResponse<AppointmentListItemDto>> updateAppointmentStatus(
            @RequestHeader(
                    value = "X-Clinic-Id",
                    required = false
            )
            Long requestedClinicId,
            @PathVariable Long appointmentId,
            @RequestBody UpdateAppointmentStatusRequest request) {

        log.info(
                "Updating appointment status. appointmentId={}, status={}",
                appointmentId,
                request.status()
        );
        Long clinicId = clinicContextResolver.resolveClinicId(requestedClinicId);
        Appointment appointment = appointmentRepository
                .findByIdAndClinicId(appointmentId,clinicId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Appointment not found: " + appointmentId
                        )
                );

        AppointmentStatus currentStatus = appointment.getStatus();
        AppointmentStatus newStatus = request.status();

        appointmentAdminService.validateStatusTransition(
                currentStatus,
                newStatus
        );

        // Synchronize queue lifecycle
        if (newStatus == AppointmentStatus.WAITING
                || newStatus == AppointmentStatus.IN_CONSULTATION
                || newStatus == AppointmentStatus.COMPLETED
                || newStatus == AppointmentStatus.CANCELLED) {

            ClinicQueueEntry queueEntry =
                    queueEntryRepository
                            .findByAppointment_Id(appointmentId)
                            .orElseThrow(() ->
                                    new ConflictException(
                                            "Queue entry not found for appointment: "
                                                    + appointmentId
                                    )
                            );

            switch (newStatus) {

                case WAITING -> queueEntry.setQueueStatus(QueueStatus.WAITING);

                case IN_CONSULTATION -> {
                    queueEntry.setQueueStatus(
                            QueueStatus.IN_CONSULTATION
                    );
                    queueEntry.setConsultationStartedAt(
                            LocalDateTime.now()
                    );
                }

                case COMPLETED -> {
                    queueEntry.setQueueStatus(QueueStatus.COMPLETED);
                    queueEntry.setCompletedAt(LocalDateTime.now());
                }

                case CANCELLED -> queueEntry.setQueueStatus(QueueStatus.CANCELLED);

                default -> {
                    // No queue update required
                }
            }

            queueEntryRepository.save(queueEntry);
        }

        appointment.setStatus(newStatus);

        Appointment savedAppointment =
                appointmentRepository.save(appointment);

        Optional<Notification> notification =
                notificationRepository.findByAppointmentIdAndTypeAndChannel(
                        savedAppointment.getId(),
                        NotificationType.REMINDER_24H,
                        NotificationChannel.WHATSAPP
                );

        notification.ifPresent(p ->
                p.setStatus(NotificationStatus.SENT)
        );
        Optional<ClinicQueueEntry> byAppointmentId = queueEntryRepository.findByAppointment_Id(appointmentId);
        AppointmentListItemDto response = toDto(savedAppointment, byAppointmentId.get());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Appointment status updated successfully.",
                        response
                )
        );
    }

    AppointmentListItemDto toDto(Appointment savedAppointment, ClinicQueueEntry queueEntry) {
        return new AppointmentListItemDto(savedAppointment.getId(),
                savedAppointment.getAppointmentCode(),
                savedAppointment.getAppointmentDate(),
                savedAppointment.getStartTime(),
                savedAppointment.getEndTime(),
                savedAppointment.getAmount(),
                savedAppointment.getPaymentStatus(),
                savedAppointment.getStatus(),
                savedAppointment.getSource() != null ? savedAppointment.getSource().name() : null,
                savedAppointment.getDoctor().getId(),
                savedAppointment.getClinic().getName(),
                savedAppointment.getDoctor().getName(),
                savedAppointment.getService().getId(),
                savedAppointment.getService().getName(),
                savedAppointment.getPatient().getName(),
                savedAppointment.getPatient().getWhatsappNumber(),
                savedAppointment.getFollowUpOfAppointment() != null ? savedAppointment.getFollowUpOfAppointment().getId() : null, savedAppointment.getSuggestedFollowUpDate(),
                queueEntry != null ? queueEntry.getId() : null,
                queueEntry != null ? queueEntry.getQueueToken() : null,
                queueEntry != null ? queueEntry.getQueueNumber() : null,
                queueEntry != null ? queueEntry.getQueueDate() : null,
                queueEntry != null ? queueEntry.getQueueStatus() : null,
                queueEntry != null ? queueEntry.getCheckedInAt() : null,
                queueEntry != null ? queueEntry.getConsultationStartedAt() : null,
                queueEntry != null ? queueEntry.getCompletedAt() : null,
                null,
                savedAppointment.getCreatedAt(),
                queueEntry != null ? queueEntry.getCancelledAt() : savedAppointment.getCancelledAt()
        );

    }

    @PreAuthorize("""
            hasAnyRole(
                'SUPER_ADMIN',
                'CLINIC_ADMIN',
                'STAFF',
                'DOCTOR'
            )
            """)
    @PatchMapping("/api/appointments/{appointmentId}/cancel")
    public ResponseEntity<ApiResponse<AppointmentListItemDto>> cancelAppointment(
            @RequestHeader(
                    value = "X-Clinic-Id",
                    required = false
            )
            Long requestedClinicId,
            @PathVariable Long appointmentId) {

        log.info(
                "Cancelling appointment. appointmentId={}",
                appointmentId
        );

        AppointmentListItemDto response =
                appointmentAdminService.cancelAppointment(
                        appointmentId
                );

        return ResponseEntity
                .ok(
                        ApiResponse.success(
                                "Appointment cancelled successfully.",
                                response
                        )
                );
    }


    @Transactional
    @PreAuthorize("""
            hasAnyRole(
                'SUPER_ADMIN',
                'CLINIC_ADMIN',
                'STAFF',
                'DOCTOR'
            )
            """)
    @PatchMapping("/api/dashboard/appointments/{appointmentId}/reschedule")
    public ResponseEntity<ApiResponse<AppointmentListItemDto>> rescheduleAppointment(
            @RequestHeader(
                    value = "X-Clinic-Id",
                    required = false
            )
            Long requestedClinicId,
            @PathVariable Long appointmentId,
            @Valid @RequestBody RescheduleAppointmentRequest request) {

        log.info(
                "Rescheduling appointment. appointmentId={}, date={}, startTime={}, endTime={}",
                appointmentId,
                request.appointmentDate(),
                request.startTime(),
                request.endTime()
        );

        AppointmentListItemDto response =
                appointmentAdminService.rescheduleAppointment(
                        appointmentId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Appointment rescheduled successfully.",
                        response
                )
        );
    }

    @Transactional
    @PreAuthorize("""
            hasAnyRole(
                'SUPER_ADMIN',
                'CLINIC_ADMIN',
                'STAFF',
                'DOCTOR'
            )
            """)
    @PatchMapping("/api/dashboard/appointments/{appointmentId}/follow-up")
    public ResponseEntity<ApiResponse<AppointmentListItemDto>> suggestFollowUp(
            @RequestHeader(
                    value = "X-Clinic-Id",
                    required = false
            )
            Long requestedClinicId,
            @PathVariable Long appointmentId,
            @Valid @RequestBody FollowUpRequest request) {

        log.info(
                "Suggesting follow-up. appointmentId={}, followUpDate={}",
                appointmentId,
                request.suggestedFollowUpDate()
        );

        AppointmentListItemDto response =
                appointmentAdminService.suggestFollowUp(
                        appointmentId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Follow-up date saved successfully.",
                        response
                )
        );
    }

    @DeleteMapping("/api/dashboard/clinics/appointments/{appointmentId}")
    @PreAuthorize("""
            hasAnyRole(
                'SUPER_ADMIN'
            )
            """)
    public ResponseEntity<ApiResponse<Void>> deleteAppointment(
            @RequestHeader(
                    value = "X-Clinic-Id",
                    required = false
            )
            Long requestedClinicId,
            @PathVariable Long appointmentId) {

        log.info(
                "Deleting appointment. clinicId={}, appointmentId={}",
                requestedClinicId,
                appointmentId
        );

        appointmentAdminService.deleteAppointment(requestedClinicId, appointmentId);

        return ResponseEntity.ok(
                ApiResponse.success("Appointment deleted successfully", null)
        );
    }
}
