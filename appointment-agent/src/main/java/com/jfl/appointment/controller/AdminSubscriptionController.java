package com.jfl.appointment.controller;


import com.jfl.appointment.dashboard.dto.ApiResponse;
import com.jfl.appointment.dto.PaymentResponse;
import com.jfl.appointment.dto.VerifyPaymentRequest;
import com.jfl.appointment.service.SubscriptionPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/subscription")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminSubscriptionController {

    private final SubscriptionPaymentService paymentService;

    /**
     * Get all payments waiting for verification.
     */
    @GetMapping("/payments/pending")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>>
    getPendingPayments() {

        List<PaymentResponse> response =
                paymentService.getPendingPayments();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Pending payments fetched successfully.",
                        response
                )
        );
    }

    /**
     * Approve / reject payment.
     */
    @PatchMapping("/payments/{paymentId}/verify")
    public ResponseEntity<ApiResponse<PaymentResponse>>
    verifyPayment(
            @PathVariable Long paymentId,
            @Valid @RequestBody VerifyPaymentRequest request
    ) {

        PaymentResponse response =
                paymentService.verifyPayment(
                        paymentId,
                        request
                );

        String message =
                Boolean.TRUE.equals(request.approved())
                        ? "Payment verified and subscription activated."
                        : "Payment rejected.";

        return ResponseEntity.ok(
                ApiResponse.success(
                        message,
                        response
                )
        );
    }

    /**
     * Get individual payment.
     */
    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>>
    getPayment(
            @PathVariable Long paymentId
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Payment fetched successfully.",
                        paymentService.getPayment(
                                paymentId
                        )
                )
        );
    }
}
