package com.jfl.appointment.dto;

import com.jfl.appointment.entity.AppointmentPaymentStatus;
import com.jfl.appointment.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AppointmentPaymentResponse(

        Long id,

        Long appointmentId,

        BigDecimal totalAmount,

        BigDecimal paidAmount,

        BigDecimal remainingAmount,

        AppointmentPaymentStatus status,

        PaymentMethod paymentMethod,

        LocalDateTime paidAt
) {
}