package com.jfl.appointment.n8n.controller;

import com.jfl.appointment.n8n.dto.DoctorDto;
import com.jfl.appointment.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/n8n/clinics/{clinicId}/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorRepository doctorRepository;

    @Cacheable(
            value = "clinicDoctors",
            key = "#clinicId + ':' + (#serviceId != null ? #serviceId : 'ALL')"
    )
    @GetMapping
    public List<DoctorDto> getDoctors(@PathVariable Long clinicId, @RequestParam(required = false) Long serviceId) {
        log.info("Get Doctors : clinicId -> {} , ServiceId -> {}", clinicId, serviceId);
        if (StringUtils.isEmpty(serviceId))
            return doctorRepository.findByClinicIdAndActiveTrue(clinicId).stream()
                    .map(d -> new DoctorDto(d.getId(), d.getName(), d.getSpecialization(), d.isActive()))
                    .toList();
        return doctorRepository.findDoctorsByClinicAndService(clinicId, serviceId).stream()
                .map(d -> new DoctorDto(d.getId(), d.getName(), d.getSpecialization(), d.isActive()))
                .toList();

    }
}
