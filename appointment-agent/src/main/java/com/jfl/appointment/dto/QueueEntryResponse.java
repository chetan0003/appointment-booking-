package com.jfl.appointment.dto;

import com.jfl.appointment.entity.QueueStatus;

import java.time.LocalDate;

public record QueueEntryResponse(
        Long appointmentId,
        Long queueEntryId,
        String queueToken,
        Integer queueNumber,
        LocalDate queueDate,
        QueueStatus queueStatus
) {}
