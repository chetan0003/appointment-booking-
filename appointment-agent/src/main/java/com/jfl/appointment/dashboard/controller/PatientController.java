package com.jfl.appointment.dashboard.controller;

import com.jfl.appointment.dashboard.dto.ApiResponse;
import com.jfl.appointment.dashboard.dto.AppointmentListItemDto;
import com.jfl.appointment.dashboard.dto.CreatePatientRequest;
import com.jfl.appointment.dashboard.dto.PatientResponseDto;
import com.jfl.appointment.dashboard.service.AppointmentAdminService;
import com.jfl.appointment.dashboard.service.PatientService;
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
@RequestMapping("/api/dashboard/clinics/{clinicId}/patients")
@RequiredArgsConstructor
public class PatientController {


    private final PatientService patientService;
    private final AppointmentAdminService appointmentAdminService;


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
            @PathVariable Long clinicId,
            @Valid @RequestBody CreatePatientRequest request) {

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
                'SUPER_ADMIN',
                'CLINIC_ADMIN',
                'STAFF',
                'DOCTOR'
            )
            """)
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PatientResponseDto>>> searchPatients(
            @PathVariable Long clinicId,
            @RequestParam String query) {

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
            @PathVariable Long clinicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

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
            @PathVariable Long clinicId,
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

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

