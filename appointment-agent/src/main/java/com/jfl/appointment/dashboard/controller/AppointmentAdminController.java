package com.jfl.appointment.dashboard.controller;

import com.jfl.appointment.dashboard.dto.*;
import com.jfl.appointment.dashboard.service.AppointmentAdminService;
import com.jfl.appointment.entity.Appointment;
import com.jfl.appointment.entity.AppointmentStatus;
import com.jfl.appointment.exception.ConflictException;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.repository.AppointmentRepository;
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
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AppointmentAdminController {

    private final AppointmentAdminService appointmentAdminService;
    private final AppointmentRepository appointmentRepository;

    @PreAuthorize("""
        hasAnyRole(
            'SUPER_ADMIN',
            'CLINIC_ADMIN',
            'STAFF',
            'DOCTOR'
        )
        """)
    @PostMapping("/api/dashboard/clinics/{clinicId}/appointments")
    public ResponseEntity<ApiResponse<AppointmentListItemDto>> createAppointment(
            @PathVariable Long clinicId,
            @Valid @RequestBody CreateAppointmentRequest request) {

        log.info(
                "Creating appointment. clinicId={}, patientId={}, doctorId={}, serviceId={}, date={}",
                clinicId,
                request.patientId(),
                request.doctorId(),
                request.serviceId(),
                request.appointmentDate()
        );

        AppointmentListItemDto response =
                appointmentAdminService.createAppointment(
                        clinicId,
                        request
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
    @GetMapping("/api/dashboard/clinics/{clinicId}/appointments")
    public ResponseEntity<ApiResponse<Page<AppointmentListItemDto>>> listAppointments(
            @PathVariable Long clinicId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from, @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to, @RequestParam(required = false)
            Long doctorId, @RequestParam(required = false)
            Long serviceId, @RequestParam(required = false)
            AppointmentStatus status,
            @PageableDefault(size = 5, sort = "appointmentDate", direction = Sort.Direction.ASC)
            Pageable pageable) {

        LocalDate resolvedFrom =
                from != null
                        ? from
                        : LocalDate.now();

        LocalDate resolvedTo =
                to != null
                        ? to
                        : resolvedFrom;

        Page<AppointmentListItemDto> appointments =
                appointmentAdminService.listAppointments(
                        clinicId,
                        resolvedFrom,
                        resolvedTo,
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
            @PathVariable Long appointmentId,
            @RequestBody UpdateAppointmentStatusRequest request) {

        log.info(
                "Updating appointment status. appointmentId={}, status={}",
                appointmentId,
                request.status()
        );

        // --------------------------------------------------
        // 1. Find appointment
        // --------------------------------------------------
        Appointment appointment = appointmentRepository
                .findById(appointmentId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Appointment not found: " + appointmentId
                        )
                );

        // --------------------------------------------------
        // 2. Validate status transition
        // --------------------------------------------------
        AppointmentStatus currentStatus =
                appointment.getStatus();

        AppointmentStatus newStatus =
                request.status();

//        if (currentStatus != AppointmentStatus.CONFIRMED && newStatus == AppointmentStatus.CANCELLED) {
//            throw new ConflictException("Only Confirmed Appointment Can Cancelled");
//        }

        appointmentAdminService.validateStatusTransition(
                currentStatus,
                newStatus
        );

        // --------------------------------------------------
        // 3. Update status
        // --------------------------------------------------
        appointment.setStatus(newStatus);

        Appointment savedAppointment =
                appointmentRepository.save(appointment);

        // --------------------------------------------------
        // 4. Convert to DTO
        // --------------------------------------------------
        AppointmentListItemDto response =
                toDto(savedAppointment);

        // --------------------------------------------------
        // 5. Generic API response
        // --------------------------------------------------
        return ResponseEntity
                .ok(
                        ApiResponse.success(
                                "Appointment status updated successfully.",
                                response
                        )
                );
    }

    AppointmentListItemDto toDto(Appointment savedAppointment) {
        return new AppointmentListItemDto(savedAppointment.getId(),
                savedAppointment.getAppointmentCode(),
                savedAppointment.getAppointmentDate(),
                savedAppointment.getStartTime(),
                savedAppointment.getEndTime(),
                savedAppointment.getStatus(),
                savedAppointment.getDoctor().getId(),
                savedAppointment.getDoctor().getName(),
                savedAppointment.getService().getId(),
                savedAppointment.getService().getName(),
                savedAppointment.getPatient().getName(),
                savedAppointment.getPatient().getWhatsappNumber(),
                savedAppointment.getFollowUpOfAppointment() != null ? savedAppointment.getFollowUpOfAppointment().getFollowUpOfAppointment().getId():null, savedAppointment.getSuggestedFollowUpDate());

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

//    @PreAuthorize("""
//                hasAnyRole(
//                    'SUPER_ADMIN',
//                    'CLINIC_ADMIN',
//                    'STAFF',
//                    'DOCTOR'
//                )
//            """)
//    @PatchMapping("/api/appointments/{appointmentId}/reschedule")
//    public AppointmentListItemDto rescheduleAppointment(
//            @PathVariable Long appointmentId,
//            @Valid @RequestBody RescheduleAppointmentRequest request) {
//        return appointmentAdminService.rescheduleAppointment(
//                appointmentId, request.appointmentDate(), request.startTime());
//    }

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
}
