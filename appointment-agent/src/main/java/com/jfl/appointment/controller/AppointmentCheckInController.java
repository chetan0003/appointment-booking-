package com.jfl.appointment.controller;

import com.jfl.appointment.dashboard.dto.ApiResponse;
import com.jfl.appointment.dto.AppointmentQrDetailsResponse;
import com.jfl.appointment.dto.QueueEntryResponse;
import com.jfl.appointment.service.AppointmentQrService;
import com.jfl.appointment.service.ClinicQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard/appointments")
@RequiredArgsConstructor
public class AppointmentCheckInController {

    private final ClinicQueueService clinicQueueService;
    private final AppointmentQrService appointmentQrService;

    @PostMapping("/{appointmentId}/check-in")
    @Transactional
    @PreAuthorize("""
            hasAnyRole(
                'SUPER_ADMIN',
                'CLINIC_ADMIN',
                'STAFF',
                'DOCTOR'
            )
            """)
    public ResponseEntity<ApiResponse<QueueEntryResponse>> checkIn(
            @PathVariable Long appointmentId) {

        QueueEntryResponse response =
                clinicQueueService.checkIn(appointmentId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Patient checked in successfully",
                        response
                )
        );
    }

    // ============================================================
    // CHECK-IN USING QR
    // ============================================================
    @PostMapping("/check-in")
    public ResponseEntity<QueueEntryResponse> checkIn(
            @RequestParam String token
    ) {

        QueueEntryResponse entryResponse =
                clinicQueueService.checkInUsingQr(token);

        return ResponseEntity.ok(
                entryResponse
        );
    }

    @GetMapping("/appointment")
    @PreAuthorize("""
            hasAnyRole(
                'SUPER_ADMIN',
                'CLINIC_ADMIN',
                'STAFF',
                'DOCTOR'
            )
            """)
    public ResponseEntity<ApiResponse<AppointmentQrDetailsResponse>>
    getAppointmentByQrToken(
            @RequestParam String token
    ) {

        AppointmentQrDetailsResponse response =
                appointmentQrService
                        .getAppointmentDetailsByToken(token);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Appointment details retrieved successfully",
                        response
                )
        );
    }

}
