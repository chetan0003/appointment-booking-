package com.jfl.appointment.repository;

import com.jfl.appointment.entity.ClinicHoliday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ClinicHolidayRepository
        extends JpaRepository<ClinicHoliday, Long> {

    Optional<ClinicHoliday> findByClinicIdAndHolidayDateAndActiveTrue(
            Long clinicId,
            LocalDate holidayDate
    );

    List<ClinicHoliday> findByClinicIdAndActiveTrueOrderByHolidayDateAsc(
            Long clinicId
    );
}
