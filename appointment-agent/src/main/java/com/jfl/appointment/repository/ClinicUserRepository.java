package com.jfl.appointment.repository;


import com.jfl.appointment.entity.ClinicUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClinicUserRepository
        extends JpaRepository<ClinicUser, Long> {

    List<ClinicUser> findByUserIdAndActiveTrue(Long userId);

    Optional<ClinicUser> findByUserIdAndClinicIdAndActiveTrue(
            Long userId,
            Long clinicId
    );

    List<ClinicUser> findByClinicIdAndActiveTrue(Long clinicId);

    Optional<ClinicUser> findByUserIdAndClinicIdAndDoctorIdAndActiveTrue(
            Long userId,
            Long clinicId,
            Long doctorId
    );
}
