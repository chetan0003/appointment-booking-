package com.jfl.appointment.dashboard.dto;

import java.time.LocalDate;

public record ClinicHolidayDto(
        Long id,
        Long clinicId,
        LocalDate holidayDate,
        boolean active
) {
}
