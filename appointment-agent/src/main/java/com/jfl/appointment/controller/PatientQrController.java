package com.jfl.appointment.controller;

import com.jfl.appointment.dashboard.dto.ApiResponse;
import com.jfl.appointment.dto.PatientQrResponse;
import com.jfl.appointment.service.PatientQrService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard/clinics")
@RequiredArgsConstructor
public class PatientQrController {

    private final PatientQrService patientQrService;


    @PostMapping("/{clinicId}/patients/{patientId}/qr")
    public ResponseEntity<ApiResponse<PatientQrResponse>> generateQr(

            @PathVariable Long clinicId,

            @PathVariable Long patientId
    ) {

        PatientQrResponse response =
                patientQrService.generateQr(
                        clinicId,
                        patientId
                );

        return ResponseEntity
                .status(201)
                .body(
                        ApiResponse.success(
                                "Patient WhatsApp QR generated successfully",
                                response
                        )
                );
    }


    @DeleteMapping("/{clinicId}/patients/{patientId}/qr")
    public ResponseEntity<ApiResponse<Void>> revokeQr(

            @PathVariable Long clinicId,

            @PathVariable Long patientId
    ) {

        patientQrService.revokeQr(
                clinicId,
                patientId
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Patient QR revoked successfully",
                        null
                )
        );
    }
}