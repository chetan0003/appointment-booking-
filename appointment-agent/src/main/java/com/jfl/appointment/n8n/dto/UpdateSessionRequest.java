package com.jfl.appointment.n8n.dto;

import com.jfl.appointment.entity.ConversationState;

import java.time.LocalDate;
import java.time.LocalTime;

// All fields optional/nullable - PATCH-style partial update.
// n8n sends only the fields it just collected from the conversation;
// anything null here is left untouched on the existing session row.
public record UpdateSessionRequest(
        String intent,
        Long serviceId,
        Long doctorId,
        LocalDate appointmentDate,
        LocalTime selectedStartTime,
        String patientName,
        ConversationState state
) {}
