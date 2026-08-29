package com.jfl.appointment.dashboard.controller;

import com.jfl.appointment.dashboard.dto.CreateDoctorAvailabilityRequest;
import com.jfl.appointment.dashboard.dto.CreateDoctorRequest;
import com.jfl.appointment.dashboard.dto.DoctorAvailabilityDto;
import com.jfl.appointment.dashboard.dto.UpdateDoctorRequest;
import com.jfl.appointment.entity.*;
import com.jfl.appointment.n8n.dto.DoctorDto;
import com.jfl.appointment.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/api/dashboard/clinics/{clinicId}/doctors")
@RequiredArgsConstructor
public class DoctorDashboardController {

    private final ClinicRepository clinicRepository;
    private final DoctorRepository doctorRepository;
    private final ServiceOfferingRepository serviceRepository;
    private final DoctorServiceRepository doctorServiceRepository;
    private final DoctorAvailabilityRepository doctorAvailabilityRepository;


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
            return doctorRepository.findByClinicId(clinicId).stream()
                    .map(d -> new DoctorDto(d.getId(), d.getName(), d.getSpecialization(), d.isActive()))
                    .toList();
        return doctorRepository.findDoctorsByClinicAndServiceForDashboard(clinicId, serviceId).stream()
                .map(d -> new DoctorDto(d.getId(), d.getName(), d.getSpecialization(), d.isActive()))
                .toList();

    }

    @PreAuthorize("""
            hasAnyRole(
                'ROLE_SUPER_ADMIN',
                'CLINIC_ADMIN'
            )
            """)
    @PostMapping("/{doctorId}/availability")
    public ResponseEntity<List<DoctorAvailabilityDto>> createOrUpdateAvailability(
            @PathVariable Long clinicId,
            @PathVariable Long doctorId,
            @RequestBody List<CreateDoctorAvailabilityRequest> requests) {

        log.info(
                "Create/Update doctor availability. clinicId={}, doctorId={}",
                clinicId,
                doctorId
        );

        // --------------------------------------------------
        // 1. Validate doctor and clinic
        // --------------------------------------------------
        Doctor doctor =
                doctorRepository
                        .findById(doctorId)
                        .filter(d ->
                                d.getClinic()
                                        .getId()
                                        .equals(clinicId)
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Doctor not found: " + doctorId
                                )
                        );

        // --------------------------------------------------
        // 2. Get existing availability
        // --------------------------------------------------
        List<DoctorAvailability> existingAvailability =
                doctorAvailabilityRepository
                        .findByDoctorIdAndActiveTrueOrderByDayOfWeekAsc(
                                doctorId
                        );

        // --------------------------------------------------
        // 3. Map existing records by day
        // --------------------------------------------------
        Map<DayOfWeek, DoctorAvailability> existingByDay =
                existingAvailability.stream()
                        .collect(Collectors.toMap(
                                DoctorAvailability::getDayOfWeek,
                                Function.identity()
                        ));

        List<DoctorAvailability> availabilityToSave =
                new ArrayList<>();

        // --------------------------------------------------
        // 4. Create / Update
        // --------------------------------------------------
        for (CreateDoctorAvailabilityRequest request : requests) {

            DayOfWeek dayOfWeek;

            try {
                dayOfWeek =
                        DayOfWeek.valueOf(
                                request.dayOfWeek().toUpperCase()
                        );
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(
                        "Invalid day of week: "
                                + request.dayOfWeek()
                );
            }

            // ----------------------------------------------
            // Validate working time
            // ----------------------------------------------
            if (request.startTime() == null
                    || request.endTime() == null) {

                throw new RuntimeException(
                        "Start time and end time are required for "
                                + dayOfWeek
                );
            }

            if (!request.startTime()
                    .isBefore(request.endTime())) {

                throw new RuntimeException(
                        "Start time must be before end time for "
                                + dayOfWeek
                );
            }

            // ----------------------------------------------
            // Validate break
            // ----------------------------------------------
            if (request.breakStartTime() != null
                    || request.breakEndTime() != null) {

                if (request.breakStartTime() == null
                        || request.breakEndTime() == null) {

                    throw new RuntimeException(
                            "Both break start and break end "
                                    + "must be provided"
                    );
                }

                if (!request.breakStartTime()
                        .isBefore(request.breakEndTime())) {

                    throw new RuntimeException(
                            "Break start time must be before "
                                    + "break end time for "
                                    + dayOfWeek
                    );
                }

                // Break must be inside doctor's working hours
                if (request.breakStartTime()
                        .isBefore(request.startTime())
                        || request.breakEndTime()
                        .isAfter(request.endTime())) {

                    throw new RuntimeException(
                            "Break time must be within doctor's "
                                    + "working hours for "
                                    + dayOfWeek
                    );
                }
            }

            // ----------------------------------------------
            // Find existing availability
            // ----------------------------------------------
            DoctorAvailability availability =
                    existingByDay.get(dayOfWeek);

            // ----------------------------------------------
            // INSERT
            // ----------------------------------------------
            if (availability == null) {

                availability =
                        new DoctorAvailability();

                availability.setDoctor(doctor);
                availability.setDayOfWeek(dayOfWeek);

                log.info(
                        "Creating doctor availability. "
                                + "doctorId={}, day={}",
                        doctorId,
                        dayOfWeek
                );

            } else {

                // ------------------------------------------
                // UPDATE
                // ------------------------------------------
                log.info(
                        "Updating doctor availability. "
                                + "id={}, doctorId={}, day={}",
                        availability.getId(),
                        doctorId,
                        dayOfWeek
                );
            }

            availability.setStartTime(
                    request.startTime()
            );

            availability.setEndTime(
                    request.endTime()
            );

            availability.setBreakStartTime(
                    request.breakStartTime()
            );

            availability.setBreakEndTime(
                    request.breakEndTime()
            );

            availability.setActive(
                    request.active() != null
                            ? request.active()
                            : true
            );

            availabilityToSave.add(availability);
        }

        // --------------------------------------------------
        // 5. Save
        // --------------------------------------------------
        List<DoctorAvailability> savedAvailability =
                doctorAvailabilityRepository
                        .saveAll(availabilityToSave);

        // --------------------------------------------------
        // 6. Convert to DTO
        // --------------------------------------------------
        List<DoctorAvailabilityDto> response =
                savedAvailability.stream()
                        .map(availability ->
                                new DoctorAvailabilityDto(
                                        availability.getId(),
                                        availability
                                                .getDoctor()
                                                .getId(),
                                        availability
                                                .getDayOfWeek()
                                                .name(),
                                        availability.getStartTime(),
                                        availability.getEndTime(),
                                        availability
                                                .getBreakStartTime(),
                                        availability
                                                .getBreakEndTime(),
                                        availability.isActive()
                                )
                        )
                        .toList();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}
