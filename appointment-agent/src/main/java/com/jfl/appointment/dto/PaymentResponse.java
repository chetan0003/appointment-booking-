package com.jfl.appointment.dto;


import com.jfl.appointment.entity.SubscriptionPaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(

        Long paymentId,

        Long clinicId,

        String clinicName,

        Long subscriptionId,

        Long planId,

        String planName,

        String transactionId,

        BigDecimal amount,

        String currency,

        SubscriptionPaymentStatus status,

        LocalDateTime paymentDate,

        LocalDateTime verifiedAt,

        String rejectionReason

) {
}