package com.jfl.appointment.dashboard.controller;

import com.jfl.appointment.dashboard.dto.*;
import com.jfl.appointment.dashboard.service.AppointmentAdminService;
import com.jfl.appointment.dashboard.service.PatientService;
import com.jfl.appointment.security.SecurityContextService;
import com.jfl.appointment.service.ClinicContextResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/dashboard/clinics/patients")
@RequiredArgsConstructor
public class PatientController {


    private final PatientService patientService;
    private final AppointmentAdminService appointmentAdminService;
    private final ClinicContextResolver clinicContextResolver;


    @PreAuthorize("""
            hasAnyRole(
                'SUPER_ADMIN',
                'CLINIC_ADMIN',
                'STAFF',
                'DOCTOR'
            )
            """)
    @PostMapping
    public ResponseEntity<ApiResponse<PatientResponseDto>> createPatient(
            @RequestHeader(
                    value = "X-Clinic-Id",
                    required = false
            )
            Long requestedClinicId,
            @Valid @RequestBody CreatePatientRequest request) {
        Long clinicId = clinicContextResolver.resolveClinicId(requestedClinicId);
        log.info(
                "Creating patient. clinicId={}, name={}, whatsapp={}",
                clinicId,
                request.name(),
                request.whatsappNumber()
        );

        PatientResponseDto response =
                patientService.createPatient(
                        clinicId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Patient created successfully.",
                                response
                        )
                );
    }

    @PreAuthorize("""
        hasAnyRole(
            'ROLE_SUPER_ADMIN',
            'ROLE_CLINIC_ADMIN',
            'ROLE_STAFF',
            'DOCTOR'
        )
        """)
    @PutMapping("/{patientId}")
    public ResponseEntity<ApiResponse<PatientResponseDto>> updatePatient(
            @RequestHeader(
                    value = "X-Clinic-Id",
                    required = false
            )
            Long requestedClinicId,
            @PathVariable Long patientId,
            @Valid @RequestBody UpdatePatientRequest request) {
        Long clinicId = clinicContextResolver.resolveClinicId(requestedClinicId);
        log.info(
                "Updating patient. clinicId={}, patientId={}, name={}, whatsapp={}",
                clinicId,
                patientId,
                request.name(),
                request.whatsappNumber()
        );

        PatientResponseDto response =
                patientService.updatePatient(
                        clinicId,
                        patientId,
                        request
                );

        return ResponseEntity
                .ok(
                        ApiResponse.success(
                                "Patient updated successfully.",
                                response
                        )
                );
    }


    @PreAuthorize("""
            hasAnyRole(
                'SUPER_ADMIN',
                'CLINIC_ADMIN',
                'STAFF',
                'DOCTOR'
            )
            """)
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PatientResponseDto>>> searchPatients(
            @RequestHeader(
                    value = "X-Clinic-Id",
                    required = false
            )
            Long requestedClinicId,
            @RequestParam String query) {
        Long clinicId = clinicContextResolver.resolveClinicId(requestedClinicId);
        log.info(
                "Searching patients. clinicId={}, query={}",
                clinicId,
                query
        );

        List<PatientResponseDto> patients =
                patientService.searchPatients(
                        clinicId,
                        query
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Patients fetched successfully.",
                        patients
                )
        );
    }

    @PreAuthorize("""
        hasAnyRole(
            'SUPER_ADMIN',
            'CLINIC_ADMIN',
            'STAFF',
            'DOCTOR'
        )
        """)
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PatientResponseDto>>> getAllPatient(
            @RequestHeader(
                    value = "X-Clinic-Id",
                    required = false
            )
            Long requestedClinicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        Long clinicId = clinicContextResolver.resolveClinicId(requestedClinicId);
        log.info(
                "Get all patients. clinicId={}, page={}, size={}",
                clinicId,
                page,
                size
        );

        Page<PatientResponseDto> patients =
                patientService.getAllPatient(
                        clinicId,
                        page,
                        size
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Patients fetched successfully.",
                        patients
                )
        );
    }

    @GetMapping("/{patientId}/appointments")
    public ResponseEntity<ApiResponse<Page<AppointmentListItemDto>>> getPatientAppointments(
            @RequestHeader(
                    value = "X-Clinic-Id",
                    required = false
            )
            Long requestedClinicId,
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("getPatientAppointments request for patientId :: {}", patientId);
        Long clinicId = clinicContextResolver.resolveClinicId(requestedClinicId);
        Page<AppointmentListItemDto> appointments =
                appointmentAdminService.getPatientAppointments(
                        clinicId,
                        patientId,
                        page,
                        size
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Patient appointments fetched successfully.",
                        appointments
                )
        );
    }
}

