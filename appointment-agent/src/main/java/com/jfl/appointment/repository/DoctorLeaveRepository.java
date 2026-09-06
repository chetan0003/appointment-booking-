package com.jfl.appointment.repository;

import com.jfl.appointment.entity.DoctorLeave;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DoctorLeaveRepository
        extends JpaRepository<DoctorLeave, Long> {

    List<DoctorLeave> findByDoctorIdAndLeaveDateAndActiveTrue(
            Long doctorId,
            LocalDate leaveDate
    );
}
