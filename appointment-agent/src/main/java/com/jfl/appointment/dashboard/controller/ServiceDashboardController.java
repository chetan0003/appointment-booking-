package com.jfl.appointment.dashboard.controller;

import com.jfl.appointment.entity.ServiceOffering;
import com.jfl.appointment.n8n.dto.ServiceDto;
import com.jfl.appointment.repository.ServiceOfferingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/clinics/{clinicId}/services")
@RequiredArgsConstructor
public class ServiceDashboardController {

    private final ServiceOfferingRepository serviceRepository;

    @GetMapping
    public List<ServiceDto> getServices(@PathVariable Long clinicId) {
        return serviceRepository.findByClinicIdAndActiveTrue(clinicId).stream()
                .map(this::toDto)
                .toList();
    }

    private ServiceDto toDto(ServiceOffering s) {
        return new ServiceDto(s.getId(), s.getName(), s.getDurationMinutes(), s.getPrice());
    }
}
