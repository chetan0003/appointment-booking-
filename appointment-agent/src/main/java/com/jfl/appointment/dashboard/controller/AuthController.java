package com.jfl.appointment.dashboard.controller;


import com.jfl.appointment.dashboard.dto.*;
import com.jfl.appointment.security.AuthService;
import com.jfl.appointment.security.SecurityContextService;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SecurityContextService securityContextService;

    @PostMapping("/login")
    public AuthLoginResponse login(
            @Valid @RequestBody LoginRequest request) {

        return authService.login(request);
    }

    @PostMapping("/2fa/verify-login")
    public AuthLoginResponse verifyTotpLogin(
            @Valid @RequestBody TotpLoginRequest request) {

        return authService.verifyTotpLogin(request);
    }

    @PostMapping("/2fa/enable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> enableTotp(
            @Valid @RequestBody TotpEnableRequest request,
            Authentication authentication) {

        Long userId = securityContextService.getCurrentUserId();
        authService.enableTotp(userId, request.code());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/2fa/setup")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TotpSetupResponse> setupTotp() {

        Long userId = securityContextService.getCurrentUserId();

        TotpSetupResponse response = authService.setupTotp(userId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/2fa/disable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> disableTotp(
            @Valid @RequestBody TotpDisableRequest request) {

        Long userId = securityContextService.getCurrentUserId();

        authService.disableTotp(userId, request);

        return ResponseEntity.noContent().build();
    }
}
