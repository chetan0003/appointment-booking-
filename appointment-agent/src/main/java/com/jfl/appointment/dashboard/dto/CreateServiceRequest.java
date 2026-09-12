package com.jfl.appointment.dashboard.dto;


import java.math.BigDecimal;

public record CreateServiceRequest(
        String name,
        Integer durationMinutes,
        BigDecimal price
) {
}
