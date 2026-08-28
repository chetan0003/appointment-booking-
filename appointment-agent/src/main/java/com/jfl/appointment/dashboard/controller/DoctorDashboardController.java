package com.jfl.appointment.dashboard.controller;

import com.jfl.appointment.dashboard.dto.CreateDoctorRequest;
import com.jfl.appointment.dashboard.dto.UpdateDoctorRequest;
import com.jfl.appointment.entity.Clinic;
import com.jfl.appointment.entity.Doctor;
import com.jfl.appointment.entity.DoctorService;
import com.jfl.appointment.entity.ServiceOffering;
import com.jfl.appointment.n8n.dto.DoctorDto;
import com.jfl.appointment.repository.ClinicRepository;
import com.jfl.appointment.repository.DoctorRepository;
import com.jfl.appointment.repository.DoctorServiceRepository;
import com.jfl.appointment.repository.ServiceOfferingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
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
    private final ServiceOfferingRepository serviceRepository;
    private final DoctorServiceRepository doctorServiceRepository;


    @Transactional
    @CacheEvict(value = "clinicDoctors", allEntries = true)
    @PreAuthorize("""
                hasAnyRole(
                    'SUPER_ADMIN',
                    'CLINIC_ADMIN'
                )
            """)
    @PostMapping
    public DoctorDto createDoctor(
            @PathVariable Long clinicId,
            @RequestBody CreateDoctorRequest request) {

        log.info(
                "Create Doctor : clinicId -> {}, name -> {}, serviceId -> {}",
                clinicId,
                request.name(),
                request.serviceId()
        );

        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() ->
                        new RuntimeException("Clinic not found: " + clinicId));

        ServiceOffering service = serviceRepository
                .findByIdAndClinicIdAndActiveTrue(
                        request.serviceId(),
                        clinicId
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Service not found or does not belong to clinic: "
                                        + request.serviceId()
                        ));

        Doctor doctor = new Doctor();
        doctor.setName(request.name());
        doctor.setSpecialization(request.specialization());
        doctor.setClinic(clinic);
        doctor.setActive(true);

        Doctor savedDoctor = doctorRepository.save(doctor);

        DoctorService doctorService = DoctorService.builder()
                .doctor(savedDoctor)
                .service(service)
                .build();

        doctorServiceRepository.save(doctorService);

        return new DoctorDto(
                savedDoctor.getId(),
                savedDoctor.getName(),
                savedDoctor.getSpecialization(),
                savedDoctor.isActive()
        );
    }

    @Transactional
    @CacheEvict(value = "clinicDoctors", allEntries = true)
    @PreAuthorize("""
                hasAnyRole(
                    'SUPER_ADMIN',
                    'CLINIC_ADMIN'
                )
            """)
    @PutMapping("/{doctorId}")
    public DoctorDto updateDoctor(
            @PathVariable Long clinicId,
            @PathVariable Long doctorId,
            @RequestBody UpdateDoctorRequest request) {

        log.info(
                "Update Doctor : clinicId -> {}, doctorId -> {}, serviceId -> {}",
                clinicId,
                doctorId,
                request.serviceId()
        );

        // 1. Find doctor belonging to this clinic
        Doctor doctor = doctorRepository
                .findByIdAndClinicId(doctorId, clinicId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Doctor not found: " + doctorId
                        ));

        // 2. Validate service belongs to same clinic
        ServiceOffering service = serviceRepository
                .findByIdAndClinicIdAndActiveTrue(
                        request.serviceId(),
                        clinicId
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Service not found or does not belong to clinic: "
                                        + request.serviceId()
                        ));

        // 3. Update doctor
        doctor.setName(request.name());
        doctor.setSpecialization(request.specialization());
        doctor.setActive(request.active());

        doctorRepository.save(doctor);

        // 4. Update doctor-service mapping
        DoctorService doctorService = doctorServiceRepository
                .findByDoctorId(doctorId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Service mapping not found for doctor: "
                                        + doctorId
                        ));

        doctorService.setService(service);

        doctorServiceRepository.save(doctorService);

        return new DoctorDto(
                doctor.getId(),
                doctor.getName(),
                doctor.getSpecialization(),
                doctor.isActive()
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
