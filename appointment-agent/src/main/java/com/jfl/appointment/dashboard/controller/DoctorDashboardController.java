package com.jfl.appointment.dashboard.controller;

import com.jfl.appointment.dashboard.dto.CreateDoctorRequest;
import com.jfl.appointment.entity.Clinic;
import com.jfl.appointment.entity.Doctor;
import com.jfl.appointment.n8n.dto.DoctorDto;
import com.jfl.appointment.repository.ClinicRepository;
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

    private final ClinicRepository clinicRepository;
    private final DoctorRepository doctorRepository;


    @PreAuthorize("""
                hasAnyRole(
                    'ROLE_SUPER_ADMIN',
                    'CLINIC_ADMIN'
                )
            """)
    @PostMapping
    public DoctorDto createDoctor(
            @PathVariable Long clinicId,
            @RequestBody CreateDoctorRequest request) {

        log.info("Create Doctor : clinicId -> {}, name -> {}",
                clinicId, request.name());

        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() ->
                        new RuntimeException("Clinic not found: " + clinicId));

        Doctor doctor = new Doctor();
        doctor.setName(request.name());
        doctor.setSpecialization(request.specialization());
        doctor.setClinic(clinic);
        doctor.setActive(true);

        Doctor savedDoctor = doctorRepository.save(doctor);

        return new DoctorDto(
                savedDoctor.getId(),
                savedDoctor.getName(),
                savedDoctor.getSpecialization(),
                savedDoctor.isActive()
        );
    }

    @PreAuthorize("""
                hasAnyRole(
                    'ROLE_SUPER_ADMIN',
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
                    .map(d -> new DoctorDto(d.getId(), d.getName(), d.getSpecialization(), d.isActive()))
                    .toList();
        return doctorRepository.findDoctorsByClinicAndService(clinicId, serviceId).stream()
                .map(d -> new DoctorDto(d.getId(), d.getName(), d.getSpecialization(), d.isActive()))
                .toList();

    }
}
