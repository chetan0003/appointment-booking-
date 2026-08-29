package com.jfl.appointment.dashboard.dto;

import java.time.LocalTime;

public record DoctorAvailabilityDto(
        Long id,
        Long doctorId,
        String dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        LocalTime breakStartTime,
        LocalTime breakEndTime,
        boolean active
) {
}
