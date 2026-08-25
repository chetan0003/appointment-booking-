package com.jfl.appointment.n8n.controller;

import com.jfl.appointment.n8n.dto.SessionResponse;
import com.jfl.appointment.n8n.dto.UpdateSessionRequest;
import com.jfl.appointment.n8n.service.ConversationSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class SessionController {

    private final ConversationSessionService sessionService;

    // n8n calls this first, every inbound message.
    @GetMapping("/api/n8n/clinics/{clinicId}/sessions/active")
    public SessionResponse getActiveSession(
            @PathVariable Long clinicId,
            @RequestParam String whatsappNumber) {
        return sessionService.findOrCreateActiveSession(clinicId, whatsappNumber);
    }

    // n8n calls this after the AI extracts new info from the message.
    @PatchMapping("/api/n8n/sessions/{sessionId}")
    public SessionResponse updateSession(
            @PathVariable Long sessionId,
            @RequestBody UpdateSessionRequest request) {
        return sessionService.updateSession(sessionId, request);
    }
}
