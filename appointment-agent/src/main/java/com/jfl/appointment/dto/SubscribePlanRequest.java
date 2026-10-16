package com.jfl.appointment.dto;

import jakarta.validation.constraints.NotNull;

public record SubscribePlanRequest(
        @NotNull Long planId
) {}
