package com.jfl.appointment.dashboard.controller;

import com.jfl.appointment.dashboard.dto.ApiResponse;
import com.jfl.appointment.dashboard.dto.CreateServiceRequest;
import com.jfl.appointment.entity.Clinic;
import com.jfl.appointment.entity.DoctorService;
import com.jfl.appointment.entity.ServiceOffering;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.n8n.dto.ServiceDto;
import com.jfl.appointment.repository.ClinicRepository;
import com.jfl.appointment.repository.DoctorServiceRepository;
import com.jfl.appointment.repository.ServiceOfferingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/dashboard/clinics/{clinicId}/services")
@RequiredArgsConstructor
public class ServiceDashboardController {

    private final CacheManager cacheManager;
    private final ServiceOfferingRepository serviceRepository;
    private final ClinicRepository clinicRepository;
    private final DoctorServiceRepository doctorServiceRepository;

    @Transactional
    @CacheEvict(
            value = {"clinicDoctors", "clinicServices"},
            allEntries = true
    )
    @PreAuthorize("""
        hasAnyRole(
            'SUPER_ADMIN',
            'CLINIC_ADMIN'
        )
        """)
    @PostMapping
    public ResponseEntity<ApiResponse<ServiceDto>> createService(
            @PathVariable Long clinicId,
            @RequestBody CreateServiceRequest request) {

        log.info(
                "Creating service. clinicId={}, serviceName={}",
                clinicId,
                request.name()
        );

        // --------------------------------------------------
        // 1. Validate clinic
        // --------------------------------------------------
        Clinic clinic =
                clinicRepository
                        .findById(clinicId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Clinic not found: " + clinicId
                                )
                        );

        // --------------------------------------------------
        // 2. Create service
        // --------------------------------------------------
        ServiceOffering service = new ServiceOffering();

        service.setClinic(clinic);
        service.setName(request.name());
        service.setDurationMinutes(request.durationMinutes());
        service.setPrice(request.price());
        service.setActive(true);

        // --------------------------------------------------
        // 3. Save
        // --------------------------------------------------
        ServiceOffering savedService =
                serviceRepository.save(service);

        // --------------------------------------------------
        // 4. Convert to DTO
        // --------------------------------------------------
        ServiceDto response =
                toDto(savedService);

        // --------------------------------------------------
        // 5. Generic API response
        // --------------------------------------------------
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Service created successfully.",
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
    @GetMapping
    public ResponseEntity<ApiResponse<List<ServiceDto>>> getServices(
            @PathVariable Long clinicId,
            @RequestParam(required = false) Long doctorId) {

        log.info(
                "Get Services : clinicId -> {}, doctorId -> {}",
                clinicId,
                doctorId
        );

        List<ServiceDto> services;

        if (doctorId != null) {

            services = doctorServiceRepository
                    .findByDoctorIdWithService(doctorId)
                    .map(DoctorService::getService)
                    .map(this::toDto)
                    .map(List::of)
                    .orElseGet(List::of);

        } else {

            services = serviceRepository
                    .findByClinicIdAndActiveTrue(clinicId)
                    .stream()
                    .map(this::toDto)
                    .toList();
        }

        return ResponseEntity
                .ok(
                        ApiResponse.success(
                                "Services fetched successfully.",
                                services
                        )
                );
    }

    @Transactional
    @DeleteMapping("/{serviceId}")
    public ResponseEntity<ApiResponse<Void>> deleteService(
            @PathVariable Long clinicId,
            @PathVariable Long serviceId) {

        ServiceOffering service = serviceRepository
                .findByIdAndClinicId(serviceId, clinicId)
                .orElseThrow(() ->
                        new NotFoundException("Service not found."));

        service.setActive(false);
        serviceRepository.save(service);

        // Evict service cache
        cacheManager.getCache("clinicServices").evict(clinicId);

        // If doctor list contains services, evict this too
        cacheManager.getCache("clinicDoctors").evict(clinicId);

        return ResponseEntity.ok(
                ApiResponse.success("Service deleted successfully.", null)
        );
    }

    private ServiceDto toDto(ServiceOffering s) {
        return new ServiceDto(s.getId(), s.getName(), s.getDurationMinutes(), s.getPrice());
    }
}
