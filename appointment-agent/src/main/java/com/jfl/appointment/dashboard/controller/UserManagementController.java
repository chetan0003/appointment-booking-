package com.jfl.appointment.dashboard.controller;


import com.jfl.appointment.dashboard.dto.ClinicUserDto;
import com.jfl.appointment.dashboard.dto.CreateUserRequest;
import com.jfl.appointment.dashboard.dto.UserResponse;
import com.jfl.appointment.entity.AppUser;
import com.jfl.appointment.dashboard.service.UserManagementService;
import com.jfl.appointment.repository.AppUserRepository;
import com.jfl.appointment.security.SecurityContextService;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
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

    @GetMapping("/{clinicId}")
    @PreAuthorize("""
        hasAnyRole('ROLE_SUPER_ADMIN','SUPER_ADMIN','CLINIC_ADMIN')
    """)
    public List<ClinicUserDto> getClinicUser(@PathVariable Long clinicId) {
        return userManagementService.getClinicUsersByClientId(clinicId);
    }

    @GetMapping
    public UserResponse getUserDetail() {
        log.info("user id");
        return userManagementService.getUserDetail();
    }

}
