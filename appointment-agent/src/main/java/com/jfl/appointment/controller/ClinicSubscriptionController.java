package com.jfl.appointment.controller;


import com.jfl.appointment.dashboard.dto.ApiResponse;
import com.jfl.appointment.dto.PaymentResponse;
import com.jfl.appointment.dto.SubmitPaymentRequest;
import com.jfl.appointment.dto.SubscribePlanRequest;
import com.jfl.appointment.dto.SubscriptionResponse;
import com.jfl.appointment.service.ClinicSubscriptionService;
import com.jfl.appointment.service.SubscriptionPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clinics/{clinicId}/subscription")
@RequiredArgsConstructor
public class ClinicSubscriptionController {

    private final SubscriptionPaymentService paymentService;
    private final ClinicSubscriptionService subscriptionService;
    @PostMapping("/payments")
    @PreAuthorize("""
        hasAnyRole(
            'SUPER_ADMIN',
            'CLINIC_ADMIN'
        )
        """)
    public ResponseEntity<ApiResponse<PaymentResponse>> submitPayment(
            @PathVariable Long clinicId,
            @Valid @RequestBody SubmitPaymentRequest request
    ) {

        PaymentResponse response =
                paymentService.submitPayment(
                        clinicId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Payment submitted successfully. " +
                                "It is pending verification.",
                        response
                )
        );
    }

    @GetMapping("/payments")
    @PreAuthorize("""
        hasAnyRole(
            'SUPER_ADMIN',
            'CLINIC_ADMIN'
        )
        """)
    public ResponseEntity<ApiResponse<List<PaymentResponse>>>
    getPayments(
            @PathVariable Long clinicId
    ) {

        List<PaymentResponse> response =
                paymentService.getClinicPayments(clinicId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Subscription payments fetched successfully.",
                        response
                )
        );
    }



    @GetMapping
    @PreAuthorize("""
        hasAnyRole(
            'SUPER_ADMIN',
            'CLINIC_ADMIN',
            'STAFF',
            'DOCTOR'
        )
        """)
    public ResponseEntity<ApiResponse<SubscriptionResponse>>
    getSubscription(
            @PathVariable Long clinicId
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Clinic subscription fetched successfully.",
                        subscriptionService.getClinicSubscription(
                                clinicId
                        )
                )
        );
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLINIC_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> subscribeToPlan(
            @PathVariable Long clinicId,
            @Valid @RequestBody SubscribePlanRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        "Subscription plan selected successfully. Please complete the payment.",
                        subscriptionService.subscribeToPlan(clinicId, request)
                )
        );
    }
}
