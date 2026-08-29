package com.jfl.appointment.dashboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateClinicHolidayRequest(

        @NotNull
        LocalDate holidayDate,

        @NotBlank
        String name
) {
}
