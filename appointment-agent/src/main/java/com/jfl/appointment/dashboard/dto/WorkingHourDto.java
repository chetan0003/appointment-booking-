package com.jfl.appointment.dashboard.dto;


import java.time.LocalTime;

public record WorkingHourDto(
        Long id,
        Long clinicId,
        String dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        LocalTime breakStartTime,
        LocalTime breakEndTime,
        boolean active
) {
}
