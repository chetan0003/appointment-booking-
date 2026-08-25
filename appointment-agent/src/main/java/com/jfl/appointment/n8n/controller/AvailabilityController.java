package com.jfl.appointment.n8n.controller;

import com.jfl.appointment.n8n.dto.AvailabilityResponse;
import com.jfl.appointment.n8n.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/n8n/clinics/{clinicId}/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping
    public AvailabilityResponse getAvailability(
            @PathVariable Long clinicId,
            @RequestParam Long doctorId,
            @RequestParam Long serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return availabilityService.getAvailability(clinicId, doctorId, serviceId, date);
    }
}
