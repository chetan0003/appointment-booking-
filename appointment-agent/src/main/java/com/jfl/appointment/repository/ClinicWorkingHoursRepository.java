package com.jfl.appointment.repository;

import com.jfl.appointment.entity.ClinicWorkingHours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.Optional;

public interface ClinicWorkingHoursRepository extends JpaRepository<ClinicWorkingHours, Long> {
    Optional<ClinicWorkingHours> findByClinicIdAndDayOfWeekAndActiveTrue(Long clinicId, DayOfWeek dayOfWeek);
    Optional<ClinicWorkingHours> findByClinic_IdAndDayOfWeekAndActiveTrue(
            Long clinicId,
            DayOfWeek dayOfWeek
    );
}
