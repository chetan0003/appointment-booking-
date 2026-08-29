package com.jfl.appointment.dashboard.controller;

import com.jfl.appointment.dashboard.dto.ApiResponse;
import com.jfl.appointment.dashboard.dto.AppointmentListItemDto;
import com.jfl.appointment.dashboard.dto.RescheduleAppointmentRequest;
import com.jfl.appointment.dashboard.dto.UpdateAppointmentStatusRequest;
import com.jfl.appointment.dashboard.service.AppointmentAdminService;
import com.jfl.appointment.entity.Appointment;
import com.jfl.appointment.entity.AppointmentStatus;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.repository.AppointmentRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public ResponseEntity<ApiResponse<List<AppointmentListItemDto>>> listAppointments(
            @PathVariable Long clinicId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from, @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to, @RequestParam(required = false)
            Long doctorId, @RequestParam(required = false)
            Long serviceId, @RequestParam(required = false)
            AppointmentStatus status) {

        LocalDate resolvedFrom =
                from != null
                        ? from
                        : LocalDate.now();

        LocalDate resolvedTo =
                to != null
                        ? to
                        : resolvedFrom;

        List<AppointmentListItemDto> appointments =
                appointmentAdminService.listAppointments(
                        clinicId,
                        resolvedFrom,
                        resolvedTo,
                        doctorId,
                        status,
                        serviceId
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
                savedAppointment.getPatient().getWhatsappNumber());
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

    @PreAuthorize("""
                hasAnyRole(
                    'SUPER_ADMIN',
                    'CLINIC_ADMIN',
                    'STAFF',
                    'DOCTOR'
                )
            """)
    @PatchMapping("/api/appointments/{appointmentId}/reschedule")
    public AppointmentListItemDto rescheduleAppointment(
            @PathVariable Long appointmentId,
            @Valid @RequestBody RescheduleAppointmentRequest request) {
        return appointmentAdminService.rescheduleAppointment(
                appointmentId, request.appointmentDate(), request.startTime());
    }
}
