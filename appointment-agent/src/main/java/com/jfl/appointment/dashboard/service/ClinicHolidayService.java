
package com.jfl.appointment.dashboard.service;

import com.jfl.appointment.dashboard.dto.ClinicHolidayDto;
import com.jfl.appointment.dashboard.dto.CreateClinicHolidayRequest;
import com.jfl.appointment.entity.Clinic;
import com.jfl.appointment.entity.ClinicHoliday;
import com.jfl.appointment.exception.ConflictException;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.repository.ClinicHolidayRepository;
import com.jfl.appointment.repository.ClinicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClinicHolidayService {

    private final ClinicHolidayRepository clinicHolidayRepository;
    private final ClinicRepository clinicRepository;

    // =========================================================
    // GET CLINIC HOLIDAYS
    // =========================================================

    @Transactional(readOnly = true)
    @Cacheable(
            value = "clinicHolidays",
            key = "#clinicId"
    )
    public List<ClinicHolidayDto> getClinicHolidays(Long clinicId) {

        log.info("Fetching clinic holidays. clinicId={}", clinicId);

        LocalDate today = LocalDate.now();

        return clinicHolidayRepository
                .findByClinicIdAndActiveTrueOrderByHolidayDateAsc(clinicId)
                .stream()
                .filter(holiday ->
                        holiday.getHolidayDate().getMonth() == today.getMonth()
                                && holiday.getHolidayDate().getYear() == today.getYear()
                )
                .map(this::toDto)
                .toList();
    }

    // =========================================================
    // CREATE CLINIC HOLIDAY
    // =========================================================

    @Transactional
    @CacheEvict(
            value = "clinicHolidays",
            key = "#clinicId"
    )
    public ClinicHolidayDto createClinicHoliday(
            Long clinicId,
            CreateClinicHolidayRequest request
    ) {

        log.info(
                "Creating clinic holiday. clinicId={}, date={}",
                clinicId,
                request.holidayDate()
        );

        // 1. Validate clinic
        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Clinic not found: " + clinicId
                        )
                );

        // 2. Prevent duplicate active holiday for the same date
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

        // 3. Create entity
        ClinicHoliday holiday = new ClinicHoliday();
        holiday.setClinic(clinic);
        holiday.setName(request.name());
        holiday.setHolidayDate(request.holidayDate());
        holiday.setActive(true);

        // 4. Save
        ClinicHoliday savedHoliday =
                clinicHolidayRepository.save(holiday);

        log.info(
                "Clinic holiday created successfully. clinicId={}, holidayId={}",
                clinicId,
                savedHoliday.getId()
        );

        // 5. Return DTO
        return toDto(savedHoliday);
    }

    // =========================================================
    // DELETE CLINIC HOLIDAY
    // =========================================================

    @Transactional
    @CacheEvict(
            value = "clinicHolidays",
            key = "#clinicId"
    )
    public void deleteClinicHoliday(Long clinicId, Long holidayId) {

        log.info(
                "Deleting clinic holiday. clinicId={}, holidayId={}",
                clinicId,
                holidayId
        );

        // Find holiday belonging to the requested clinic
        ClinicHoliday holiday = clinicHolidayRepository
                .findByIdAndClinicId(holidayId, clinicId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Clinic holiday not found: " + holidayId
                        )
                );

        // Hard delete, matching the existing controller behavior
        clinicHolidayRepository.delete(holiday);

        log.info(
                "Clinic holiday deleted successfully. clinicId={}, holidayId={}",
                clinicId,
                holidayId
        );
    }

    // =========================================================
    // ENTITY TO DTO MAPPING
    // =========================================================

    private ClinicHolidayDto toDto(ClinicHoliday holiday) {

        return new ClinicHolidayDto(
                holiday.getId(),
                holiday.getName(),
                holiday.getClinic().getId(),
                holiday.getHolidayDate(),
                holiday.isActive()
        );
    }
}
