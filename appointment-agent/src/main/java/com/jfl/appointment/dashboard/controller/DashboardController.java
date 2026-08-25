package com.jfl.appointment.dashboard.controller;

import com.jfl.appointment.dashboard.dto.ApiErrorResponse;
import com.jfl.appointment.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/{clinicId}")
    @PreAuthorize("""
        hasAnyRole(
            'SUPER_ADMIN',
            'CLINIC_ADMIN',
            'STAFF',
            'DOCTOR'
        )
    """)
    public ApiErrorResponse.DashboardResponse getDashboard(
            @PathVariable Long clinicId) {

        return dashboardService.getDashboard(
                clinicId
        );
    }
}
