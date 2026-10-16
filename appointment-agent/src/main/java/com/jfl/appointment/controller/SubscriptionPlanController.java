package com.jfl.appointment.controller;


import com.jfl.appointment.dashboard.dto.ApiResponse;
import com.jfl.appointment.dto.SubscriptionPlanResponse;
import com.jfl.appointment.entity.SubscriptionPlan;
import com.jfl.appointment.service.SubscriptionPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/subscription-plans")
@RequiredArgsConstructor
public class SubscriptionPlanController {

    private final SubscriptionPlanService planService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SubscriptionPlanResponse>>>
    getActivePlans() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Subscription plans fetched successfully.",
                        planService.getActivePlans()
                )
        );
    }

    @GetMapping("/{planId}")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>>
    getPlan(
            @PathVariable Long planId
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Subscription plan fetched successfully.",
                        planService.getPlan(planId)
                )
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>>
    createPlan(
            @RequestBody SubscriptionPlan plan
    ) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Subscription plan created successfully.",
                                planService.createPlan(plan)
                        )
                );
    }

    @PutMapping("/{planId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>>
    updatePlan(
            @PathVariable Long planId,
            @RequestBody SubscriptionPlan plan
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Subscription plan updated successfully.",
                        planService.updatePlan(
                                planId,
                                plan
                        )
                )
        );
    }

    @PatchMapping("/{planId}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>>
    updateStatus(
            @PathVariable Long planId,
            @RequestParam boolean active
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Subscription plan status updated successfully.",
                        planService.updateStatus(
                                planId,
                                active
                        )
                )
        );
    }
}
