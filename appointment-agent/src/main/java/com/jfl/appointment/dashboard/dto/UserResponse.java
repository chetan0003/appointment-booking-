package com.jfl.appointment.dashboard.dto;

import java.util.List;

public record UserResponse(

        Long id,
        String username,
        String email,
        String firstName,
        String lastName,

        String role,
        String phone,
        boolean enabled,
        List<ClinicResponse> clinic

) {}
