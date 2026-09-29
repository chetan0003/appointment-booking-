package com.jfl.appointment.service;

import com.jfl.appointment.entity.AppointmentStatus;
import com.jfl.appointment.entity.ClinicSubscription;
import com.jfl.appointment.entity.SubscriptionPlan;
import com.jfl.appointment.entity.SubscriptionStatus;
import com.jfl.appointment.exception.SubscriptionLimitExceededException;
import com.jfl.appointment.exception.SubscriptionRequiredException;
import com.jfl.appointment.repository.AppointmentRepository;
import com.jfl.appointment.repository.ClinicSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SubscriptionLimitService {

    private final ClinicSubscriptionRepository subscriptionRepository;
    private final AppointmentRepository appointmentRepository;

    public void validateAppointmentLimit(Long clinicId) {

        ClinicSubscription subscription =
                subscriptionRepository
                        .findByClinicId(clinicId)
                        .orElseThrow(() ->
                                new SubscriptionRequiredException(
                                        "No subscription found for clinic"
                                ));

        if (!isSubscriptionActive(subscription)) {
            throw new SubscriptionRequiredException(
                    "Your subscription is not active"
            );
        }

        SubscriptionPlan plan =
                subscription.getPlan();

        Integer limit =
                plan.getMaxAppointmentsPerMonth();

        // null or negative can mean unlimited
        if (limit == null || limit < 0) {
            return;
        }

        LocalDate startDate =
                subscription.getStartDate();

        LocalDate endDate =
                subscription.getEndDate();

        long appointmentCount =
                appointmentRepository.countAppointmentsForPeriod(
                        clinicId,
                        startDate,
                        endDate,
                        AppointmentStatus.CANCELLED
                );

        if (appointmentCount >= limit) {

            throw new SubscriptionLimitExceededException(
                    "Appointment limit reached. " +
                            "Your " + plan.getName() +
                            " plan allows only " +
                            limit +
                            " appointments per subscription period."
            );
        }
    }

    private boolean isSubscriptionActive(
            ClinicSubscription subscription
    ) {

        LocalDate today = LocalDate.now();

        if (subscription.getStatus()
                == SubscriptionStatus.ACTIVE) {

            return subscription.getStartDate() != null
                    && subscription.getEndDate() != null
                    && !today.isBefore(
                    subscription.getStartDate()
            )
                    && !today.isAfter(
                    subscription.getEndDate()
            );
        }

        if (subscription.getStatus()
                == SubscriptionStatus.TRIALING) {

            return subscription.getTrialEndDate() != null
                    && !today.isAfter(
                    subscription.getTrialEndDate()
            );
        }

        return false;
    }
}
