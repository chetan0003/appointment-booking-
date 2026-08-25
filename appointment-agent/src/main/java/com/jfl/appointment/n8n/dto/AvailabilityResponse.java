package com.jfl.appointment.n8n.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record AvailabilityResponse(LocalDate date, List<LocalTime> slots) {}
