package com.jfl.appointment.controller;

import com.jfl.appointment.dashboard.dto.ApiResponse;
import com.jfl.appointment.dto.ClinicQrResponse;
import com.jfl.appointment.service.ClinicQrService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/clinics")
@RequiredArgsConstructor
public class ClinicQrController {


    private final ClinicQrService clinicQrService;

    @PostMapping("/{clinicId}/qr")
    public ResponseEntity<ApiResponse<ClinicQrResponse>> generateQr(

            @PathVariable Long clinicId
    ) {

        ClinicQrResponse response =
                clinicQrService.generateQr(
                        clinicId
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
}
