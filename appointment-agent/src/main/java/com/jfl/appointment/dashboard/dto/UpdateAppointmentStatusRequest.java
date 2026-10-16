package com.jfl.appointment.dashboard.dto;

import com.jfl.appointment.entity.AppointmentStatus;

public record UpdateAppointmentStatusRequest(
        AppointmentStatus status
) {
}
