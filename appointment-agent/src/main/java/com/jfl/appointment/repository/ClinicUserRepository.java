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

    @Query("""
                SELECT cu
                FROM ClinicUser cu
                JOIN FETCH cu.clinic
                WHERE cu.user.id = :userId
            """)
    Optional<ClinicUser> findByUserIdWithClinic(@Param("userId") Long userId);

    @Query("""
                SELECT COUNT(DISTINCT u)
                FROM ClinicUser u
                JOIN u.user.roles r
                WHERE u.clinic.id = :clinicId
                  AND r.name = :roleName
            """)
    long countUsersByClinicAndRole(
            @Param("clinicId") Long clinicId,
            @Param("roleName") String roleName
    );


    //==================================================================
           //reciepient
    //==================================================================
    @Query(value = """
        SELECT DISTINCT cu.user_id
        FROM clinic_user cu
        JOIN app_user au
            ON au.id = cu.user_id
        JOIN user_role ur
            ON ur.user_id = au.id
        JOIN role r
            ON r.id = ur.role_id
        WHERE cu.clinic_id = :clinicId
          AND cu.active = true
          AND au.enabled = true
          AND (
              r.name IN ('CLINIC_ADMIN', 'STAFF')
              OR (
                  r.name = 'DOCTOR'
                  AND cu.doctor_id = :doctorId
              )
          )
        """, nativeQuery = true)
    List<Long> findAppointmentRecipients(
            @Param("clinicId") Long clinicId,
            @Param("doctorId") Long doctorId
    );
}
