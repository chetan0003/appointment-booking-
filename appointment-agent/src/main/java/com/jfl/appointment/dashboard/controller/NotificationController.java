package com.jfl.appointment.dashboard.controller;


import com.jfl.appointment.dashboard.dto.ApiResponse;
import com.jfl.appointment.dashboard.dto.NotificationDto;
import com.jfl.appointment.dashboard.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/appointments")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PreAuthorize("""
            hasAnyRole(
                'SUPER_ADMIN',
                'CLINIC_ADMIN',
                'STAFF',
                'DOCTOR'
            )
            """)
    @GetMapping("/{appointmentId}/notifications")
    public ResponseEntity<ApiResponse<List<NotificationDto>>>
    getAppointmentNotifications(
            @PathVariable Long appointmentId) {

        List<NotificationDto> notifications =
                notificationService
                        .getAppointmentNotifications(
                                appointmentId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Notifications fetched successfully.",
                        notifications
                )
        );
    }
}
