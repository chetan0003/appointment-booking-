package com.jfl.appointment.dashboard.controller;

import com.jfl.appointment.dashboard.dto.ApiResponse;
import com.jfl.appointment.dashboard.service.DashboardAvailabilityService;
import com.jfl.appointment.n8n.dto.AvailabilityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard/clinics/{clinicId}/availability")
@RequiredArgsConstructor
public class AvailabilityDashboardController {

    private final DashboardAvailabilityService dashboardAvailabilityService;

    @PreAuthorize("""
        hasAnyRole(
            'SUPER_ADMIN',
            'CLINIC_ADMIN',
            'STAFF',
            'DOCTOR'
        )
        """)
    @GetMapping
    public ResponseEntity<ApiResponse<AvailabilityResponse>> getAvailability(
            @PathVariable Long clinicId,
            @RequestParam Long doctorId,
            @RequestParam Long serviceId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        AvailabilityResponse availability =
                dashboardAvailabilityService.getAvailability(
                        clinicId,
                        doctorId,
                        serviceId,
                        date
                );

        return ResponseEntity
                .ok(
                        ApiResponse.success(
                                "Availability fetched successfully.",
                                availability
                        )
                );
    }
}
