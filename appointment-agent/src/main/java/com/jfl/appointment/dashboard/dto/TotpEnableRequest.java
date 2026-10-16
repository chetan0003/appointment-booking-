package com.jfl.appointment.dashboard.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record TotpEnableRequest(
        @Min(0)
        @Max(999999)
        int code
) {}