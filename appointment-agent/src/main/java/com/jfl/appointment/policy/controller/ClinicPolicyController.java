package com.jfl.appointment.policy.controller;


import com.jfl.appointment.policy.dto.ClinicPolicyRequest;
import com.jfl.appointment.policy.dto.ClinicPolicyResponse;
import com.jfl.appointment.policy.dto.PolicyValidationResponse;
import com.jfl.appointment.policy.dto.PolicyVersionResponse;
import com.jfl.appointment.policy.entity.PolicyCategory;
import com.jfl.appointment.policy.service.ClinicPolicyService;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard/clinics/{clinicId}/policies")
@RequiredArgsConstructor
public class ClinicPolicyController {

    private final ClinicPolicyService policyService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN')")
    public ResponseEntity<List<ClinicPolicyResponse>> getPolicies(
            @PathVariable Long clinicId) {

        return ResponseEntity.ok(
                policyService.getPolicies(clinicId)
        );
    }

    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN')")
    public ResponseEntity<List<ClinicPolicyResponse>> getByCategory(
            @PathVariable Long clinicId,
            @PathVariable PolicyCategory category) {

        return ResponseEntity.ok(
                policyService.getPoliciesByCategory(
                        clinicId, category
                )
        );
    }

    @GetMapping("/{policyId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN')")
    public ResponseEntity<ClinicPolicyResponse> getPolicy(
            @PathVariable Long clinicId,
            @PathVariable Long policyId) {

        return ResponseEntity.ok(
                policyService.getPolicy(clinicId, policyId)
        );
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN')")
    public ResponseEntity<ClinicPolicyResponse> createPolicy(
            @PathVariable Long clinicId,
            @Valid @RequestBody ClinicPolicyRequest request,
            Principal principal) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        policyService.createPolicy(
                                clinicId,
                                request,
                                principal.getName()
                        )
                );
    }

    @PutMapping("/{policyId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN')")
    public ResponseEntity<ClinicPolicyResponse> updateDraft(
            @PathVariable Long clinicId,
            @PathVariable Long policyId,
            @Valid @RequestBody ClinicPolicyRequest request,
            Principal principal) {

        return ResponseEntity.ok(
                policyService.updateDraft(
                        clinicId,
                        policyId,
                        request,
                        principal.getName()
                )
        );
    }

    @PostMapping("/{policyId}/validate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN')")
    public ResponseEntity<PolicyValidationResponse> validate(
            @PathVariable Long clinicId,
            @PathVariable Long policyId) {

        return ResponseEntity.ok(
                policyService.validatePolicy(clinicId, policyId)
        );
    }

    @PostMapping("/{policyId}/activate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN')")
    public ResponseEntity<ClinicPolicyResponse> activate(
            @PathVariable Long clinicId,
            @PathVariable Long policyId,
            @RequestBody(required = false)
            Map<String, String> body,
            Principal principal) {

        String reason = body == null
                ? "Policy activated"
                : body.getOrDefault(
                "reason", "Policy activated"
        );

        return ResponseEntity.ok(
                policyService.activatePolicy(
                        clinicId,
                        policyId,
                        principal.getName(),
                        reason
                )
        );
    }

    @GetMapping("/{policyId}/versions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN')")
    public ResponseEntity<List<PolicyVersionResponse>> versions(
            @PathVariable Long clinicId,
            @PathVariable Long policyId) {

        return ResponseEntity.ok(
                policyService.getVersionHistory(
                        clinicId, policyId
                )
        );
    }

    @PostMapping("/{policyId}/rollback/{versionNumber}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN')")
    public ResponseEntity<ClinicPolicyResponse> rollback(
            @PathVariable Long clinicId,
            @PathVariable Long policyId,
            @PathVariable Integer versionNumber,
            Principal principal) {

        return ResponseEntity.ok(
                policyService.rollback(
                        clinicId,
                        policyId,
                        versionNumber,
                        principal.getName()
                )
        );
    }

    @PostMapping("/{policyId}/deactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN')")
    public ResponseEntity<Void> deactivate(
            @PathVariable Long clinicId,
            @PathVariable Long policyId,
            Principal principal) {

        policyService.deactivatePolicy(
                clinicId,
                policyId,
                principal.getName()
        );

        return ResponseEntity.noContent().build();
    }
}
