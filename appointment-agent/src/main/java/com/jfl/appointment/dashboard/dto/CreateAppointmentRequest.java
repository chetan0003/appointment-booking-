package com.jfl.appointment.dashboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record CreateAppointmentRequest(
        @NotNull Long clinicId,
        @NotNull Long doctorId,
        @NotNull Long serviceId,
        @NotBlank String patientName,
        @NotBlank String whatsappNumber,
        @NotNull LocalDate appointmentDate,
        @NotNull LocalTime startTime,
        // Optional client-supplied idempotency key (n8n should pass the WhatsApp
        // message id here so webhook retries don't create duplicate bookings).
        String idempotencyKey,
        // Optional - if supplied, this conversation_session is marked BOOKED
        // on success, freeing the patient to start a new conversation.
        Long sessionId
) {}
