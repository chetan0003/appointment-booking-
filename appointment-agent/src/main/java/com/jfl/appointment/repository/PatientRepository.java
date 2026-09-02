package com.jfl.appointment.repository;

import com.jfl.appointment.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    List<Patient> findByClinicId(Long clinicId);

    Optional<Patient> findByClinicIdAndWhatsappNumber(Long clinicId, String whatsappNumber);

    Optional<Patient> findByClinicIdAndWhatsappNumberAndName(
            Long clinicId,
            String whatsappNumber,
            String name
    );

    boolean existsByClinicIdAndWhatsappNumber(
            Long clinicId,
            String whatsappNumber
    );

    long countByClinicId(Long clinicId);


    @Query("""
            SELECT p
            FROM Patient p
            WHERE p.clinic.id = :clinicId
             
              AND (
                    LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR p.whatsappNumber LIKE CONCAT('%', :query, '%')
                  )
            ORDER BY p.name ASC
            """)
    List<Patient> searchPatients(
            @Param("clinicId") Long clinicId,
            @Param("query") String query
    );

    Page<Patient> findByClinicId(
            Long clinicId,
            Pageable pageable
    );
}
