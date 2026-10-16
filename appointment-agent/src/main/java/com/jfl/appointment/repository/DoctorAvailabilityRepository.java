package com.jfl.appointment.repository;

import com.jfl.appointment.entity.DoctorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface DoctorAvailabilityRepository
        extends JpaRepository<DoctorAvailability, Long> {

    Optional<DoctorAvailability>
    findByDoctorIdAndDayOfWeekAndActiveTrue(
            Long doctorId,
            DayOfWeek dayOfWeek
    );

    List<DoctorAvailability> findByDoctorIdAndActiveTrue(
            Long doctorId
    );


    List<DoctorAvailability>
    findByDoctorIdAndActiveTrueOrderByDayOfWeekAsc(
            Long doctorId
    );
}
