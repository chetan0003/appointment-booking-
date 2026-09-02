package com.jfl.appointment.repository;

import com.jfl.appointment.entity.ServiceOffering;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, Long> {
    List<ServiceOffering> findByClinicIdAndActiveTrue(Long clinicId);
    Optional<ServiceOffering> findByIdAndClinicIdAndActiveTrue(
            Long id,
            Long clinicId
    );

    Optional<ServiceOffering> findByIdAndClinicId(
            Long serviceId,
            Long clinicId
    );
}
