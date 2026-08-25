package com.jfl.appointment.repository;

import com.jfl.appointment.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    List<Doctor> findByClinicIdAndActiveTrue(Long clinicId);

    // Now genuinely filters via the doctor_service join, instead of the
    // caller's serviceId param being silently ignored.
//    List<Doctor> findByClinicIdAndActiveTrueAndServices_Id(Long clinicId, Long serviceId);
    @Query(value = """
            SELECT d.*
            FROM doctor d
            INNER JOIN doctor_service ds
                ON ds.doctor_id = d.id
            WHERE d.clinic_id = :clinicId
              AND ds.service_id = :serviceId
              AND d.active = true
            """, nativeQuery = true)
    List<Doctor> findDoctorsByClinicAndService(
            @Param("clinicId") Long clinicId,
            @Param("serviceId") Long serviceId
    );

    long countByClinicIdAndActiveTrue(Long clinicId);

}
