package com.jfl.appointment.service;


import com.jfl.appointment.dto.SubscriptionPlanResponse;
import com.jfl.appointment.entity.SubscriptionPlan;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository planRepository;

    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> getActivePlans() {

        return planRepository
                .findByActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubscriptionPlanResponse getPlan(Long planId) {

        SubscriptionPlan plan =
                getPlanEntity(planId);

        return toResponse(plan);
    }

    @Transactional(readOnly = true)
    public SubscriptionPlan getPlanEntity(Long planId) {

        return planRepository.findById(planId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Subscription plan not found: " + planId
                        ));
    }

    public SubscriptionPlanResponse createPlan(
            SubscriptionPlan plan
    ) {

        if (planRepository
                .findByCode(plan.getCode())
                .isPresent()) {

            throw new IllegalStateException(
                    "Subscription plan already exists: "
                            + plan.getCode()
            );
        }

        SubscriptionPlan saved =
                planRepository.save(plan);

        return toResponse(saved);
    }

    public SubscriptionPlanResponse updatePlan(
            Long planId,
            SubscriptionPlan request
    ) {

        SubscriptionPlan existing =
                getPlanEntity(planId);

        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setMonthlyPrice(request.getMonthlyPrice());
        existing.setYearlyPrice(request.getYearlyPrice());

        existing.setMaxDoctors(
                request.getMaxDoctors()
        );

        existing.setMaxStaff(
                request.getMaxStaff()
        );

        existing.setMaxAppointmentsPerMonth(
                request.getMaxAppointmentsPerMonth()
        );

        existing.setMaxPatients(
                request.getMaxPatients()
        );

        existing.setAiReceptionistEnabled(
                request.isAiReceptionistEnabled()
        );

        existing.setWhatsappEnabled(
                request.isWhatsappEnabled()
        );

        existing.setAnalyticsEnabled(
                request.isAnalyticsEnabled()
        );

        return toResponse(existing);
    }

    public SubscriptionPlanResponse updateStatus(
            Long planId,
            boolean active
    ) {

        SubscriptionPlan plan =
                getPlanEntity(planId);

        plan.setActive(active);

        return toResponse(plan);
    }

    private SubscriptionPlanResponse toResponse(
            SubscriptionPlan plan
    ) {

        return new SubscriptionPlanResponse(
                plan.getId(),
                plan.getCode(),
                plan.getName(),
                plan.getDescription(),
                plan.getMonthlyPrice(),
                plan.getYearlyPrice(),
                plan.getMaxDoctors(),
                plan.getMaxStaff(),
                plan.getMaxAppointmentsPerMonth(),
                plan.getMaxPatients(),
                plan.isAiReceptionistEnabled(),
                plan.isWhatsappEnabled(),
                plan.isAnalyticsEnabled(),
                plan.isActive()
        );
    }
}
