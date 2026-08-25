package com.jfl.appointment.dashboard.controller;

import com.jfl.appointment.dashboard.dto.AppointmentListItemDto;
import com.jfl.appointment.dashboard.dto.RescheduleAppointmentRequest;
import com.jfl.appointment.dashboard.service.AppointmentAdminService;
import com.jfl.appointment.entity.AppointmentStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class AppointmentAdminController {

    private final AppointmentAdminService appointmentAdminService;

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
    public List<AppointmentListItemDto> listAppointments(
            @PathVariable Long clinicId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) AppointmentStatus status) {
        LocalDate resolvedFrom = from != null ? from : LocalDate.now();
        LocalDate resolvedTo = to != null ? to : resolvedFrom;
        return appointmentAdminService.listAppointments(clinicId, resolvedFrom, resolvedTo, doctorId, status, serviceId);
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
    public AppointmentListItemDto cancelAppointment(@PathVariable Long appointmentId) {
        return appointmentAdminService.cancelAppointment(appointmentId);
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
