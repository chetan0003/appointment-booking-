package com.jfl.appointment.repository;


import com.jfl.appointment.entity.ClinicUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
                SELECT cu
                FROM ClinicUser cu
                JOIN FETCH cu.user
                WHERE cu.clinic.id = :clientId
            """)
    List<ClinicUser> findByClientId(@Param("clientId") Long clientId);

    Optional<ClinicUser> findByUser_Id(Long userId);
}
