package com.jfl.appointment.service;


import com.jfl.appointment.dto.SubscribePlanRequest;
import com.jfl.appointment.dto.SubscriptionResponse;
import com.jfl.appointment.entity.*;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.repository.ClinicRepository;
import com.jfl.appointment.repository.AppointmentRepository;
import com.jfl.appointment.repository.ClinicSubscriptionRepository;
import com.jfl.appointment.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class ClinicSubscriptionService {

    private final ClinicSubscriptionRepository subscriptionRepository;
    private final ClinicRepository clinicRepository;
    private final AppointmentRepository appointmentRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    @Transactional(readOnly = true)
    public SubscriptionResponse getClinicSubscription(
            Long clinicId
    ) {

        ClinicSubscription subscription =
                getSubscription(clinicId);

        return toResponse(subscription);
    }

    @Transactional(readOnly = true)
    public ClinicSubscription getSubscription(
            Long clinicId
    ) {

        return subscriptionRepository
                .findByClinicIdAndStatus(clinicId,SubscriptionStatus.ACTIVE)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Subscription not found for clinic: "
                                        + clinicId
                        ));
    }

    @Transactional(readOnly = true)
    public boolean hasActiveSubscription(
            Long clinicId
    ) {

        ClinicSubscription subscription =
                subscriptionRepository
                        .findByClinicId(clinicId)
                        .orElse(null);

        if (subscription == null) {
            return false;
        }

        return isActive(subscription);
    }

    public void activateSubscription(
            ClinicSubscription subscription
    ) {

        LocalDate startDate =
                LocalDate.now();

        subscription.setStatus(
                SubscriptionStatus.ACTIVE
        );

        subscription.setStartDate(
                startDate
        );

        subscription.setEndDate(
                startDate.plusMonths(1)
        );

        subscription.setAutoRenew(false);
    }

    private boolean isActive(
            ClinicSubscription subscription
    ) {

        if (subscription.getStatus()
                != SubscriptionStatus.ACTIVE) {

            return false;
        }

        LocalDate today =
                LocalDate.now();

        if (subscription.getStartDate() == null) {
            return false;
        }

        if (subscription.getEndDate() == null) {
            return !today.isBefore(
                    subscription.getStartDate()
            );
        }

        return !today.isBefore(
                subscription.getStartDate()
        ) && !today.isAfter(
                subscription.getEndDate()
        );
    }

    private SubscriptionResponse toResponse(
            ClinicSubscription subscription
    ) {

        SubscriptionPlan plan =
                subscription.getPlan();

        int limit =
                plan.getMaxAppointmentsPerMonth();

        int used =
                getAppointmentsUsed(subscription);

        int remaining =
                Math.max(
                        limit - used,
                        0
                );

        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getClinic().getId(),
                plan.getId(),
                plan.getCode().name(),
                plan.getName(),
                plan.getMonthlyPrice(),
                subscription.getStatus(),
                subscription.getStartDate(),
                subscription.getEndDate(),
                subscription.getTrialEndDate(),
                subscription.isAutoRenew(),
                limit,
                used,
                remaining
        );
    }

    private int getAppointmentsUsed(
            ClinicSubscription subscription
    ) {

        if (subscription.getStartDate() == null) {
            return 0;
        }

        LocalDate start =
                subscription.getStartDate();

        LocalDate end =
                subscription.getEndDate() != null
                        ? subscription.getEndDate()
                        : LocalDate.now();

        return Math.toIntExact(
                appointmentRepository
                        .countAppointmentsForPeriod(
                                subscription.getClinic().getId(),
                                start,
                                end
                        )
        );
    }


    public SubscriptionResponse subscribeToPlan(
            Long clinicId,
            SubscribePlanRequest request) {

        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new NotFoundException(
                        "Clinic not found: " + clinicId
                ));

        SubscriptionPlan plan = subscriptionPlanRepository.findById(request.planId())
                .orElseThrow(() -> new NotFoundException(
                        "Subscription plan not found: " + request.planId()
                ));

        if (!plan.isActive()) {
            throw new IllegalStateException(
                    "Selected subscription plan is not active."
            );
        }

        ClinicSubscription subscription =
                subscriptionRepository.findByClinicId(clinicId)
                        .orElseGet(() -> {
                            ClinicSubscription newSubscription =
                                    new ClinicSubscription();

                            newSubscription.setClinic(clinic);
                            newSubscription.setPlan(plan);
                            newSubscription.setStatus(SubscriptionStatus.PENDING);
                            newSubscription.setAutoRenew(false);

                            return newSubscription;
                        });

        subscription.setPlan(plan);

        // Payment is required before activation
        subscription.setStatus(SubscriptionStatus.PENDING);
        subscription.setStartDate(null);
        subscription.setEndDate(null);

        ClinicSubscription saved =
                subscriptionRepository.save(subscription);

        return toResponse(saved);
    }

}
