package com.jfl.appointment.dashboard.controller;


import com.jfl.appointment.dashboard.dto.ApiResponse;
import com.jfl.appointment.dashboard.dto.MarkFailedRequest;
import com.jfl.appointment.dashboard.dto.NotificationDueDto;
import com.jfl.appointment.dashboard.service.NotificationDispatchService;
import com.jfl.appointment.dashboard.service.NotificationService;
import com.jfl.appointment.dto.AppNotificationResponse;
import com.jfl.appointment.entity.NotificationChannel;
import com.jfl.appointment.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationDispatchService dispatchService;
    private final EmailService emailService;


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


    //=================================================================
                 //IN-APP NOTIFICATION
   //==================================================================

    @GetMapping("/clinic/{clinicId}/notifications")
    public ResponseEntity<ApiResponse<Page<AppNotificationResponse>>> getNotifications(
            @PathVariable Long clinicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Notifications fetched successfully.",
                        notificationService.getNotifications(
                                clinicId, pageable
                        )
                )
        );
    }

    @GetMapping("/clinic/{clinicId}/unread/count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @PathVariable Long clinicId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Unread notification count fetched successfully.",
                        notificationService.getUnreadCount(clinicId)
                )
        );
    }

    @PatchMapping("/clinic/{clinicId}/notification/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long clinicId,
            @PathVariable Long notificationId) {

        notificationService.markAsRead(clinicId, notificationId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Notification marked as read.",
                        null
                )
        );
    }

    @PatchMapping("/clinic/{clinicId}/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @PathVariable Long clinicId) {

        notificationService.markAllAsRead(clinicId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "All notifications marked as read.",
                        null
                )
        );
    }

    //===================================================================================================

    @PostMapping("/brevo-test-email")
    public ResponseEntity<String> sendTestEmail() {

        emailService.sendSubscriptionActivatedEmail("chetan.dahule03@gmail.com",
                "Chetan","MultiSpeciality Clinic","Chetan", LocalDate.now(),LocalDate.now().plusDays(30));

        return ResponseEntity.ok("Email sent successfully");
    }
}
