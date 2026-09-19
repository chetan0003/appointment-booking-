package com.jfl.appointment.n8n.controller;


import com.jfl.appointment.dashboard.dto.ApiResponse;
import com.jfl.appointment.n8n.dto.CreateWhatsAppConfigRequest;
import com.jfl.appointment.n8n.dto.WhatsAppConfigResponse;
import com.jfl.appointment.n8n.service.ClinicWhatsAppConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard/clinics/{clinicId}/whatsapp-config")
@RequiredArgsConstructor
public class ClinicWhatsAppConfigController {

    private final ClinicWhatsAppConfigService whatsappConfigService;

    @PostMapping
    public ResponseEntity<ApiResponse<WhatsAppConfigResponse>> createConfig(
            @PathVariable Long clinicId,
            @Valid @RequestBody CreateWhatsAppConfigRequest request) {

        WhatsAppConfigResponse response =
                whatsappConfigService.createConfig(clinicId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "WhatsApp configuration created successfully.",
                                response
                        )
                );
    }
}