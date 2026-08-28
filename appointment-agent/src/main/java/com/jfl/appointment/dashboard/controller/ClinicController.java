package com.jfl.appointment.dashboard.controller;

import com.jfl.appointment.dashboard.dto.ClinicResponse;
import com.jfl.appointment.dashboard.dto.CreateClinicRequest;
import com.jfl.appointment.dashboard.dto.CreateWorkingHourRequest;
import com.jfl.appointment.dashboard.dto.WorkingHourDto;
import com.jfl.appointment.dashboard.service.ClinicService;
import com.jfl.appointment.entity.Clinic;
import com.jfl.appointment.entity.ClinicWorkingHours;
import com.jfl.appointment.repository.ClinicRepository;
import com.jfl.appointment.repository.ClinicWorkingHoursRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/api/dashboard/clinics")
@RequiredArgsConstructor
public class ClinicController {


    private final ClinicService clinicService;
    private final ClinicRepository clinicRepository;
    private final ClinicWorkingHoursRepository clinicWorkingHoursRepository;

    @PreAuthorize("""
                hasAnyRole(
                    'ROLE_SUPER_ADMIN'
                )
            """)
    @PostMapping
    public ResponseEntity<ClinicResponse> createClinic(
            @RequestBody CreateClinicRequest request) {

        ClinicResponse response = clinicService.createClinic(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PreAuthorize("""
                hasAnyRole(
                    'ROLE_SUPER_ADMIN',
                    'CLINIC_ADMIN'
                )
            """)
    @GetMapping
    public ResponseEntity<List<ClinicResponse>> getAllClinic() {
        List<ClinicResponse> allClinic = clinicService.getAllClinic();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(allClinic);
    }

    @PreAuthorize("""
            hasAnyRole(
                'ROLE_SUPER_ADMIN',
                'CLINIC_ADMIN'
            )
            """)
    @PostMapping("/{clinicId}/working-hours")
    public List<WorkingHourDto> createOrUpdateWorkingHours(
            @PathVariable Long clinicId,
            @RequestBody List<CreateWorkingHourRequest> requests) {

        log.info("Create/Update working hours for clinicId: {}", clinicId);

        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() ->
                        new RuntimeException("Clinic not found: " + clinicId));

        // Get existing working hours for this clinic
        List<ClinicWorkingHours> existingWorkingHours =
                clinicWorkingHoursRepository
                        .findByClinic_IdAndActiveTrue(clinicId);

        // Map existing records by day
        Map<DayOfWeek, ClinicWorkingHours> existingByDay =
                existingWorkingHours.stream()
                        .collect(Collectors.toMap(
                                ClinicWorkingHours::getDayOfWeek,
                                Function.identity()
                        ));

        List<ClinicWorkingHours> workingHoursToSave = new ArrayList<>();

        for (CreateWorkingHourRequest request : requests) {

            DayOfWeek dayOfWeek =
                    DayOfWeek.valueOf(request.dayOfWeek().toUpperCase());

            ClinicWorkingHours workingHour =
                    existingByDay.get(dayOfWeek);

            // INSERT
            if (workingHour == null) {

                workingHour = new ClinicWorkingHours();

                workingHour.setClinic(clinic);
                workingHour.setDayOfWeek(dayOfWeek);

                log.info(
                        "Creating working hour for clinicId={}, day={}",
                        clinicId,
                        dayOfWeek
                );

            } else {

                // UPDATE
                log.info(
                        "Updating working hour id={}, clinicId={}, day={}",
                        workingHour.getId(),
                        clinicId,
                        dayOfWeek
                );
            }

            workingHour.setStartTime(request.startTime());
            workingHour.setEndTime(request.endTime());
            workingHour.setBreakStartTime(request.breakStartTime());
            workingHour.setBreakEndTime(request.breakEndTime());

            workingHour.setActive(
                    request.active() != null
                            ? request.active()
                            : true
            );

            workingHoursToSave.add(workingHour);
        }

        List<ClinicWorkingHours> savedHours =
                clinicWorkingHoursRepository.saveAll(workingHoursToSave);

        return savedHours.stream()
                .map(hour -> new WorkingHourDto(
                        hour.getId(),
                        hour.getClinic().getId(),
                        hour.getDayOfWeek().name(),
                        hour.getStartTime(),
                        hour.getEndTime(),
                        hour.getBreakStartTime(),
                        hour.getBreakEndTime(),
                        hour.isActive()
                ))
                .toList();
    }

}
