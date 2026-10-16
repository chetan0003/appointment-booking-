package com.jfl.appointment.dto;



import com.jfl.appointment.entity.SubscriptionPlanCode;

import java.math.BigDecimal;

public record SubscriptionPlanResponse(

        Long id,

        SubscriptionPlanCode code,

        String name,

        String description,

        BigDecimal monthlyPrice,

        BigDecimal yearlyPrice,

        Integer maxDoctors,

        Integer maxStaff,

        Integer maxAppointmentsPerMonth,

        Integer maxPatients,

        boolean aiReceptionistEnabled,

        boolean whatsappEnabled,

        boolean analyticsEnabled,

        boolean active
) {
}
