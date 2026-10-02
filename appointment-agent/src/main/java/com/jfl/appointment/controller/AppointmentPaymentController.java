package com.jfl.appointment.controller;

import com.jfl.appointment.dto.AppointmentPaymentResponse;
import com.jfl.appointment.dto.CollectPaymentRequest;
import com.jfl.appointment.service.AppointmentPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentPaymentController {

    private final AppointmentPaymentService paymentService;

    @PostMapping("/{appointmentId}/payment")
    public ResponseEntity<AppointmentPaymentResponse> collectPayment(
            @PathVariable Long appointmentId,
            @Valid @RequestBody CollectPaymentRequest request
    ) {

        return ResponseEntity.ok(
                paymentService.collectPayment(
                        appointmentId,
                        request
                )
        );
    }

    @GetMapping("/{appointmentId}/payment")
    public ResponseEntity<AppointmentPaymentResponse> getPayment(
            @PathVariable Long appointmentId
    ) {

        return ResponseEntity.ok(
                paymentService.getPayment(appointmentId)
        );
    }
}