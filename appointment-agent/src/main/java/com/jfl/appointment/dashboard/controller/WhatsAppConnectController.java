package com.jfl.appointment.dashboard.controller;


import com.jfl.appointment.dashboard.dto.WhatsAppConnectRequest;
import com.jfl.appointment.dashboard.service.WhatsAppIntegrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppConnectController {

    private final WhatsAppIntegrationService whatsAppService;

    public WhatsAppConnectController(WhatsAppIntegrationService whatsAppService) {
        this.whatsAppService = whatsAppService;
    }

    @PostMapping("/connect")
    public ResponseEntity<?> connectWhatsApp(@RequestBody WhatsAppConnectRequest request) {
        try {
            whatsAppService.connectWhatsApp(request.code(), request.clinicId());
            return ResponseEntity.ok(Map.of("success", true, "message", "WhatsApp integrated successfully!"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
