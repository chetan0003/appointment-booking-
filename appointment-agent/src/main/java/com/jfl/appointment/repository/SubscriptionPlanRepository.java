package com.jfl.appointment.repository;


import com.jfl.appointment.entity.SubscriptionPlan;
import com.jfl.appointment.entity.SubscriptionPlanCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPlanRepository
        extends JpaRepository<SubscriptionPlan, Long> {

    Optional<SubscriptionPlan> findByCode(
            SubscriptionPlanCode code
    );

    List<SubscriptionPlan> findByActiveTrue();
}
