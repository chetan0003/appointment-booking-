package com.jfl.appointment.dashboard.controller;


import com.jfl.appointment.dashboard.dto.LoginRequest;
import com.jfl.appointment.dashboard.dto.LoginResponse;
import com.jfl.appointment.security.AuthService;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request) {

        return authService.login(request);
    }
}
