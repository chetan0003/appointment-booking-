package com.jfl.appointment.dashboard.controller;

import com.jfl.appointment.dashboard.dto.*;
import com.jfl.appointment.dashboard.service.ClinicService;
import com.jfl.appointment.entity.Clinic;
import com.jfl.appointment.entity.ClinicHoliday;
import com.jfl.appointment.entity.ClinicWorkingHours;
import com.jfl.appointment.exception.ConflictException;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.repository.ClinicHolidayRepository;
import com.jfl.appointment.repository.ClinicRepository;
import com.jfl.appointment.repository.ClinicWorkingHoursRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    private final ClinicHolidayRepository clinicHolidayRepository;
    private final ClinicWorkingHoursRepository clinicWorkingHoursRepository;

    @PreAuthorize("""
        hasAnyRole(
            'SUPER_ADMIN'
        )
    """)
    @PostMapping
    public ResponseEntity<ApiResponse<ClinicResponse>> createClinic(
            @RequestBody CreateClinicRequest request) {

        log.info("Creating clinic");
        // --------------------------------------------------
        // Check duplicate clinic
        // --------------------------------------------------

        boolean alreadyExists =
                clinicRepository
                        .existsByNameIgnoreCaseOrWhatsappNumber(
                                request.name(),
                                request.whatsappNumber()
                        );

        if (alreadyExists) {

            throw new ConflictException(
                    "Clinic with the same name or WhatsApp number already exists."
            );
        }
        ClinicResponse response =
                clinicService.createClinic(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Clinic created successfully.",
                                response
                        )
                );
    }

    @PreAuthorize("""
        hasAnyRole(
            'SUPER_ADMIN',
            'CLINIC_ADMIN'
        )
        """)
    @GetMapping
    public ResponseEntity<ApiResponse<List<ClinicResponse>>> getAllClinic() {

        List<ClinicResponse> allClinic =
                clinicService.getAllClinic();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        ApiResponse.success(
                                "Clinics fetched successfully.",
                                allClinic
                        )
                );
    }

    @PreAuthorize("""
        hasAnyRole(
            'SUPER_ADMIN',
            'CLINIC_ADMIN'
        )
        """)
    @PostMapping("/{clinicId}/working-hours")
    public ResponseEntity<ApiResponse<List<WorkingHourDto>>> createOrUpdateWorkingHours(
            @PathVariable Long clinicId,
            @RequestBody List<CreateWorkingHourRequest> requests) {

        log.info(
                "Create/Update working hours for clinicId: {}",
                clinicId
        );

        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Clinic not found: " + clinicId
                        ));

        // Get existing active working hours for this clinic
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

        List<ClinicWorkingHours> workingHoursToSave =
                new ArrayList<>();

        for (CreateWorkingHourRequest request : requests) {

            DayOfWeek dayOfWeek =
                    DayOfWeek.valueOf(
                            request.dayOfWeek().toUpperCase()
                    );

            ClinicWorkingHours workingHour =
                    existingByDay.get(dayOfWeek);

            // =====================================================
            // INSERT
            // =====================================================

            if (workingHour == null) {

                workingHour = new ClinicWorkingHours();

                workingHour.setClinic(clinic);
                workingHour.setDayOfWeek(dayOfWeek);

                log.info(
                        "Creating working hour. clinicId={}, day={}",
                        clinicId,
                        dayOfWeek
                );

            }
            // =====================================================
            // UPDATE
            // =====================================================
            else {

                log.info(
                        "Updating working hour. id={}, clinicId={}, day={}",
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
                clinicWorkingHoursRepository.saveAll(
                        workingHoursToSave
                );

        List<WorkingHourDto> response =
                savedHours.stream()
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

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        ApiResponse.success(
                                "Clinic working hours created/updated successfully.",
                                response
                        )
                );
    }

    @PreAuthorize("""
        hasAnyRole(
            'SUPER_ADMIN',
            'CLINIC_ADMIN'
        )
        """)
    @Cacheable(
            value = "clinicHolidays",
            key = "#clinicId"
    )
    @GetMapping("/{clinicId}/holidays")
    public ResponseEntity<ApiResponse<List<ClinicHolidayDto>>> getClinicHolidays(
            @PathVariable Long clinicId) {

        List<ClinicHolidayDto> holidays =
                clinicHolidayRepository
                        .findByClinicIdAndActiveTrueOrderByHolidayDateAsc(
                                clinicId
                        )
                        .stream()
                        .map(holiday -> new ClinicHolidayDto(
                                holiday.getId(),
                                holiday.getName(),
                                holiday.getClinic().getId(),
                                holiday.getHolidayDate(),
                                holiday.isActive()
                        ))
                        .toList();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        ApiResponse.success(
                                "Clinic holidays fetched successfully.",
                                holidays
                        )
                );
    }

    @PreAuthorize("""
        hasAnyRole(
            'SUPER_ADMIN',
            'CLINIC_ADMIN'
        )
        """)
    @CacheEvict(
            value = "clinicHolidays",
            key = "#clinicId"
    )
    @PostMapping("/{clinicId}/holidays/create")
    public ResponseEntity<ApiResponse<ClinicHolidayDto>> createClinicHoliday(
            @PathVariable Long clinicId,
            @Valid @RequestBody CreateClinicHolidayRequest request) {

        log.info(
                "Creating clinic holiday. clinicId={}, date={}",
                clinicId,
                request.holidayDate()
        );

        // --------------------------------------------------
        // 1. Validate clinic
        // --------------------------------------------------
        Clinic clinic = clinicRepository
                .findById(clinicId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Clinic not found: " + clinicId
                        ));

        // --------------------------------------------------
        // 2. Check duplicate holiday
        // --------------------------------------------------
        boolean alreadyExists =
                clinicHolidayRepository
                        .findByClinicIdAndHolidayDateAndActiveTrue(
                                clinicId,
                                request.holidayDate()
                        )
                        .isPresent();

        if (alreadyExists) {
            throw new ConflictException(
                    "Holiday already exists for date: "
                            + request.holidayDate()
            );
        }

        // --------------------------------------------------
        // 3. Create holiday
        // --------------------------------------------------
        ClinicHoliday holiday = new ClinicHoliday();

        holiday.setClinic(clinic);
        holiday.setHolidayDate(request.holidayDate());
        holiday.setName(request.name());
        holiday.setActive(true);

        // --------------------------------------------------
        // 4. Save
        // --------------------------------------------------
        ClinicHoliday savedHoliday =
                clinicHolidayRepository.save(holiday);

        // --------------------------------------------------
        // 5. Convert to DTO
        // --------------------------------------------------
        ClinicHolidayDto response =
                new ClinicHolidayDto(
                        savedHoliday.getId(),
                        savedHoliday.getName(),
                        savedHoliday.getClinic().getId(),
                        savedHoliday.getHolidayDate(),
                        savedHoliday.isActive()
                );

        // --------------------------------------------------
        // 6. Generic API response
        // --------------------------------------------------
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Clinic holiday created successfully.",
                                response
                        )
                );
    }
}
