package com.jfl.appointment.repository;


import com.jfl.appointment.entity.ClinicSubscription;
import com.jfl.appointment.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClinicSubscriptionRepository
        extends JpaRepository<ClinicSubscription, Long> {

    Optional<ClinicSubscription> findByClinicIdAndStatus(Long clinicId, SubscriptionStatus status);
    Optional<ClinicSubscription> findByClinicId(Long clinicId);

    boolean existsByClinicId(Long clinicId);
}
