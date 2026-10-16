package com.jfl.appointment.n8n.controller;

import com.jfl.appointment.dashboard.dto.ApiResponse;
import com.jfl.appointment.dto.QueueEntryResponse;
import com.jfl.appointment.dto.WhatsAppQrResolveResponse;
import com.jfl.appointment.entity.*;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.repository.AppointmentQrCredentialRepository;
import com.jfl.appointment.repository.AppointmentRepository;
import com.jfl.appointment.repository.ClinicQueueEntryRepository;
import com.jfl.appointment.service.AppointmentQrService;
import com.jfl.appointment.service.AppointmentQrTokenService;
import com.jfl.appointment.service.ClinicQueueService;
import com.jfl.appointment.service.PatientQrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/n8n/patient")
@RequiredArgsConstructor
public class N8nPatientQrController {

    private final PatientQrService patientQrService;
    private final AppointmentQrService appointmentQrService;
    private final ClinicQueueService clinicQueueService;


    @PostMapping("/resolve-qr")
    public ResponseEntity<ApiResponse<WhatsAppQrResolveResponse>>
    resolveQr(
            @RequestBody QrResolveRequest request
    ) {

        WhatsAppQrResolveResponse response =
                patientQrService.resolveQr(
                        request.token()
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        response.valid()
                                ? "Patient identified successfully"
                                : "Invalid QR",
                        response
                )
        );
    }

    @GetMapping(
            value = "/appointment-qr/{token}",
            produces = MediaType.IMAGE_PNG_VALUE
    )
    public ResponseEntity<byte[]> getAppointmentQrImage(
            @PathVariable String token
    ) {

        byte[] qr =
                appointmentQrService.generateQrImage(token);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(
                        CacheControl.noCache()
                )
                .body(qr);
    }


    public record QrResolveRequest(
            String token
    ) {
    }
}