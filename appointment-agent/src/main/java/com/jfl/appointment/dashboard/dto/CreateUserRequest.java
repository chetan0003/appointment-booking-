package com.jfl.appointment.dashboard.dto;

import com.jfl.appointment.entity.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(

        @NotBlank
        String username,

        @Email
        String email,

        @NotBlank
        String password,

        @NotBlank
        String firstName,

        String lastName,

        String phone,

        @NotNull
        RoleName role,

        Long clinicId,

        Long doctorId

) {}
