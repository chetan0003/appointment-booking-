package com.jfl.appointment.dashboard.dto;

public record UserResponse(

        Long id,
        String username,
        String email,
        String firstName,
        String lastName,

        String role,
        String phone,
        boolean enabled

) {}
