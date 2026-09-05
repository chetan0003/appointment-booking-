package com.jfl.appointment.dashboard.dto;

import java.time.LocalTime;

public record CreateWorkingHourRequest(
        Long clinicId,
        String dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        LocalTime breakStartTime,
        LocalTime breakEndTime,
        Boolean active
) {
}
