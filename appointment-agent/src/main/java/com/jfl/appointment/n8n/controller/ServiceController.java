package com.jfl.appointment.n8n.controller;

import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.n8n.dto.ServiceDto;
import com.jfl.appointment.entity.ServiceOffering;
import com.jfl.appointment.repository.DoctorServiceRepository;
import com.jfl.appointment.repository.ServiceOfferingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/n8n/clinics/{clinicId}/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceOfferingRepository serviceRepository;
    private final DoctorServiceRepository doctorServiceRepository;

    @Cacheable(
            value = "clinicServices",
            key = "#clinicId"
    )
    @GetMapping
    public List<ServiceDto> getServices(@PathVariable Long clinicId) {
        log.info("Get Service : clinicId -> {} ", clinicId);
        return serviceRepository.findByClinicIdAndActiveTrue(clinicId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    @GetMapping("/doctor/{doctorId}")
    public ServiceDto getServicesByDoctor(@PathVariable Long doctorId) {

        log.info("Get Service : doctorId -> {}", doctorId);

        return doctorServiceRepository.findByDoctorId(doctorId)
                .map(p -> toDto(p.getService()))
                .orElseThrow(() ->
                        new NotFoundException(
                                "Service not found for doctorId: " + doctorId
                        )
                );
    }

    private ServiceDto toDto(ServiceOffering s) {
        return new ServiceDto(s.getId(), s.getName(), s.getDurationMinutes(), s.getPrice());
    }
}
