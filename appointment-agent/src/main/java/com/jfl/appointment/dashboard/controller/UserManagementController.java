package com.jfl.appointment.dashboard.controller;


import com.jfl.appointment.dashboard.dto.CreateUserRequest;
import com.jfl.appointment.entity.AppUser;
import com.jfl.appointment.dashboard.service.UserManagementService;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clinic-admin/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService userManagementService;

    @PostMapping
    @PreAuthorize("""
        hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN')
    """)
    public AppUser createUser(
            @Valid @RequestBody CreateUserRequest request) {

        return userManagementService.createUser(
                request
        );
    }
}
