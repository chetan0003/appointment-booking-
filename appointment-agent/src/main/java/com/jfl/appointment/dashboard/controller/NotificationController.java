package com.jfl.appointment.dashboard.controller;


import com.jfl.appointment.dashboard.dto.ApiResponse;
import com.jfl.appointment.dashboard.dto.MarkFailedRequest;
import com.jfl.appointment.dashboard.dto.NotificationDto;
import com.jfl.appointment.dashboard.dto.NotificationDueDto;
import com.jfl.appointment.dashboard.service.NotificationDispatchService;
import com.jfl.appointment.dashboard.service.NotificationService;
import com.jfl.appointment.entity.NotificationChannel;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationDispatchService dispatchService;

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


    // n8n's dispatch poll calls this on a schedule (e.g. every 15 min).
    @GetMapping("/notifications/due")
    public List<NotificationDueDto> getDue(@RequestParam(defaultValue = "WHATSAPP") NotificationChannel channel) {
        return dispatchService.findDue(channel);
    }

    @PostMapping("/notifications/{notificationId}/mark-sent")
    public void markSent(@PathVariable Long notificationId) {
        dispatchService.markSent(notificationId);
    }

    @PostMapping("/notifications/{notificationId}/mark-failed")
    public void markFailed(@PathVariable Long notificationId, @Valid @RequestBody MarkFailedRequest request) {
        dispatchService.markFailed(notificationId, request.errorMessage());
    }
}
