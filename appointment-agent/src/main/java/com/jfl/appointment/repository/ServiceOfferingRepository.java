package com.jfl.appointment.repository;

import com.jfl.appointment.entity.ServiceOffering;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, Long> {
    List<ServiceOffering> findByClinicIdAndActiveTrue(Long clinicId);
}
