package com.jfl.appointment.repository;

import com.jfl.appointment.entity.DoctorService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DoctorServiceRepository
        extends JpaRepository<DoctorService, Long> {

    Optional<DoctorService> findByDoctorId(Long doctorId);
}
