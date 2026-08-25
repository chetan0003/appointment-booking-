package com.jfl.appointment.n8n.dto;

import java.math.BigDecimal;

public record ServiceDto(Long id, String name, Integer durationMinutes, BigDecimal price) {}
