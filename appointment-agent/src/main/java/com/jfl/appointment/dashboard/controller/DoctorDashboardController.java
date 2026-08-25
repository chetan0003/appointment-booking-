package com.jfl.appointment.dashboard.controller;

import com.jfl.appointment.n8n.dto.DoctorDto;
import com.jfl.appointment.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/dashboard/clinics/{clinicId}/doctors")
@RequiredArgsConstructor
public class DoctorDashboardController {

    private final DoctorRepository doctorRepository;

    @PreAuthorize("""
                hasAnyRole(
                    'SUPER_ADMIN',
                    'CLINIC_ADMIN',
                    'STAFF',
                    'DOCTOR'
                )
            """)
    @GetMapping
    public List<DoctorDto> getDoctors(@PathVariable Long clinicId, @RequestParam(required = false) Long serviceId) {
        log.info("Get Doctors : clinicId -> {} , ServiceId -> {}", clinicId, serviceId);
        if (StringUtils.isEmpty(serviceId))
            return doctorRepository.findByClinicIdAndActiveTrue(clinicId).stream()
                    .map(d -> new DoctorDto(d.getId(), d.getName(), d.getSpecialization()))
                    .toList();
        return doctorRepository.findDoctorsByClinicAndService(clinicId, serviceId).stream()
                .map(d -> new DoctorDto(d.getId(), d.getName(), d.getSpecialization()))
                .toList();

    }
}
