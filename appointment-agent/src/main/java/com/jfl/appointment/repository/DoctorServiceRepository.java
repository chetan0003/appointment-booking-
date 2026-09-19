package com.jfl.appointment.repository;

import com.jfl.appointment.entity.DoctorService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DoctorServiceRepository
        extends JpaRepository<DoctorService, Long> {

    Optional<DoctorService> findByDoctorId(Long doctorId);

    //Optional<List<DoctorService>> findByDoctorId(Long doctorId);
    @Query("""
                SELECT ds
                FROM DoctorService ds
                JOIN FETCH ds.service
                WHERE ds.doctor.id = :doctorId
            """)
    Optional<DoctorService> findByDoctorIdWithService(
            @Param("doctorId") Long doctorId);

    boolean existsByDoctorIdAndServiceId(
            Long doctorId,
            Long serviceId
    );

    @Modifying
    @Query("DELETE FROM DoctorService ds WHERE ds.service.id = :serviceId")
    void deleteByServiceId(@Param("serviceId") Long serviceId);
}
