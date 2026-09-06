package com.jfl.appointment.dashboard.dto;

import java.time.Instant;
import java.util.List;

public record ApiDashboardResponse(String code, String message, Instant timestamp) {
    public ApiDashboardResponse(String code, String message) {
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
