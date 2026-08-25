package com.jfl.appointment.dashboard.dto;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(String code, String message, Instant timestamp) {
    public ApiErrorResponse(String code, String message) {
        this(code, message, Instant.now());
    }

    public static record DashboardResponse(

            long todayAppointments,

            long pendingAppointments,

            long totalPatients,

            long activeDoctors,

            List<AppointmentDashboardItem> todaySchedule

    ) {}
}
