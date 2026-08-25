package com.jfl.appointment.dashboard.dto;

import java.math.BigDecimal;

public record ServiceDto(Long id, String name, Integer durationMinutes, BigDecimal price) {}
