package com.jfl.appointment.dashboard.controller;

import com.jfl.appointment.dashboard.dto.ApiDashboardResponse;
import com.jfl.appointment.dashboard.dto.ApiResponse;
import com.jfl.appointment.dashboard.dto.WeeklyAppointmentDto;
import com.jfl.appointment.dashboard.service.AppointmentAdminService;
import com.jfl.appointment.dashboard.service.DashboardService;
import com.jfl.appointment.security.SecurityContextService;
import com.jfl.appointment.service.ClinicContextResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final AppointmentAdminService appointmentAdminService;
    private final SecurityContextService securityContextService;
    private final ClinicContextResolver contextResolver;

    @GetMapping
    @PreAuthorize("""
        hasAnyRole(
            'SUPER_ADMIN',
            'CLINIC_ADMIN',
            'STAFF',
            'DOCTOR'
        )
    """)
    public ApiDashboardResponse.DashboardResponse getDashboard(
            @RequestHeader(
                    value = "X-Clinic-Id",
                    required = false
            )
            Long requestedClinicId
    ) {
        Long clinicId = contextResolver.resolveClinicId(requestedClinicId);
        return dashboardService.getDashboard(
                clinicId
        );
    }

    @GetMapping(
            "/clinics/appointments-this-week"
    )
    @PreAuthorize("""
        hasAnyRole(
            'SUPER_ADMIN',
            'CLINIC_ADMIN',
            'STAFF',
            'DOCTOR'
        )
    """)
    public ResponseEntity<ApiResponse<List<WeeklyAppointmentDto>>>
    getAppointmentsThisWeek(
            @RequestHeader(
                    value = "X-Clinic-Id",
                    required = false
            )
            Long requestedClinicId
    ) {
        Long clinicId = contextResolver.resolveClinicId(requestedClinicId);
        List<WeeklyAppointmentDto> response =
                appointmentAdminService
                        .getAppointmentsThisWeek(clinicId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Weekly appointment statistics fetched successfully.",
                        response
                )
        );
    }
}
