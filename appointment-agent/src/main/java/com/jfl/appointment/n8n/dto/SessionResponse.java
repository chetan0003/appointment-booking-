package com.jfl.appointment.n8n.dto;

import com.jfl.appointment.entity.ConversationState;

import java.time.LocalDate;
import java.time.LocalTime;

public record SessionResponse(
        Long sessionId,
        Long clinicId,
        String whatsappNumber,
        String intent,
        Long serviceId,
        Long doctorId,
        LocalDate appointmentDate,
        LocalTime selectedStartTime,
        String patientName,
        ConversationState state
) {}
