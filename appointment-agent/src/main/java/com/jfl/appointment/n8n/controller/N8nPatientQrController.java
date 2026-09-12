package com.jfl.appointment.n8n.controller;

import com.jfl.appointment.dashboard.dto.ApiResponse;
import com.jfl.appointment.dto.WhatsAppQrResolveResponse;
import com.jfl.appointment.service.PatientQrService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/n8n/patient")
@RequiredArgsConstructor
public class N8nPatientQrController {

    private final PatientQrService patientQrService;


    @PostMapping("/resolve-qr")
    public ResponseEntity<ApiResponse<WhatsAppQrResolveResponse>>
    resolveQr(
            @RequestBody QrResolveRequest request
    ) {

        WhatsAppQrResolveResponse response =
                patientQrService.resolveQr(
                        request.token()
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        response.valid()
                                ? "Patient identified successfully"
                                : "Invalid QR",
                        response
                )
        );
    }


    public record QrResolveRequest(
            String token
    ) {
    }
}