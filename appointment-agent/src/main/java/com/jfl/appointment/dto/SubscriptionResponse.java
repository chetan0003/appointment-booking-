package com.jfl.appointment.dto;


import com.jfl.appointment.entity.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionResponse(

        Long subscriptionId,

        Long clinicId,

        Long planId,

        String planCode,

        String planName,

        BigDecimal monthlyPrice,

        SubscriptionStatus status,

        LocalDate startDate,

        LocalDate endDate,

        LocalDate trialEndDate,

        boolean autoRenew,

        Integer appointmentLimit,

        Integer appointmentsUsed,

        Integer appointmentsRemaining
) {
}
