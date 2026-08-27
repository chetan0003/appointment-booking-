package com.jfl.appointment.dashboard.controller;

import com.jfl.appointment.dashboard.dto.ClinicResponse;
import com.jfl.appointment.dashboard.dto.CreateClinicRequest;
import com.jfl.appointment.dashboard.service.ClinicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/dashboard/clinics")
@RequiredArgsConstructor
public class ClinicController {


    private final ClinicService clinicService;

    @PreAuthorize("""
                hasAnyRole(
                    'ROLE_SUPER_ADMIN',
                    'CLINIC_ADMIN'
                )
            """)
    @PostMapping
    public ResponseEntity<ClinicResponse> createClinic(
            @RequestBody CreateClinicRequest request) {

        ClinicResponse response = clinicService.createClinic(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PreAuthorize("""
                hasAnyRole(
                    'ROLE_SUPER_ADMIN',
                    'CLINIC_ADMIN'
                )
            """)
    @GetMapping
    public ResponseEntity<List<ClinicResponse>> getAllClinic() {
        List<ClinicResponse> allClinic = clinicService.getAllClinic();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(allClinic);
    }

}
