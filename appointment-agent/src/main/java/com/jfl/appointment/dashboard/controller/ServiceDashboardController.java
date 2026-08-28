package com.jfl.appointment.dashboard.controller;

import com.jfl.appointment.dashboard.dto.CreateServiceRequest;
import com.jfl.appointment.entity.Clinic;
import com.jfl.appointment.entity.DoctorService;
import com.jfl.appointment.entity.ServiceOffering;
import com.jfl.appointment.n8n.dto.ServiceDto;
import com.jfl.appointment.repository.ClinicRepository;
import com.jfl.appointment.repository.DoctorServiceRepository;
import com.jfl.appointment.repository.ServiceOfferingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/dashboard/clinics/{clinicId}/services")
@RequiredArgsConstructor
public class ServiceDashboardController {

    private final ServiceOfferingRepository serviceRepository;
    private final ClinicRepository clinicRepository;
    private final DoctorServiceRepository doctorServiceRepository;

    @PostMapping
    @CacheEvict(value = {"clinicDoctors", "clinicServices"}, allEntries = true)
    public ServiceDto createService(
            @PathVariable Long clinicId,
            @RequestBody CreateServiceRequest request) {

        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() ->
                        new RuntimeException("Clinic not found: " + clinicId));

        ServiceOffering service = new ServiceOffering();
        service.setClinic(clinic);
        service.setName(request.name());
        service.setDurationMinutes(request.durationMinutes());
        service.setPrice(request.price());
        service.setActive(true);

        ServiceOffering savedService = serviceRepository.save(service);

        return toDto(savedService);
    }

    //@Transactional(readOnly = true)
    @GetMapping
    public List<ServiceDto> getServices(
            @PathVariable Long clinicId,
            @RequestParam(required = false) Long doctorId) {

        if (doctorId != null) {

            return doctorServiceRepository
                    .findByDoctorIdWithService(doctorId)
                    .map(DoctorService::getService)
                    .map(this::toDto)
                    .map(List::of)
                    .orElseGet(List::of);
        }

        return serviceRepository
                .findByClinicIdAndActiveTrue(clinicId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private ServiceDto toDto(ServiceOffering s) {
        return new ServiceDto(s.getId(), s.getName(), s.getDurationMinutes(), s.getPrice());
    }
}
