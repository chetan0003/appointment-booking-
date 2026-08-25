package com.jfl.appointment.dashboard.controller;

import com.jfl.appointment.dashboard.service.DashboardAvailabilityService;
import com.jfl.appointment.n8n.dto.AvailabilityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard/clinics/{clinicId}/availability")
@RequiredArgsConstructor
public class AvailabilityDashboardController {

    private final DashboardAvailabilityService dashboardAvailabilityService;

    @GetMapping
    public AvailabilityResponse getAvailability(
            @PathVariable Long clinicId,
            @RequestParam Long doctorId,
            @RequestParam Long serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dashboardAvailabilityService.getAvailability(clinicId, doctorId, serviceId, date);
    }
}
