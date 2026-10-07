package com.jfl.appointment.policy.dto;


import com.fasterxml.jackson.databind.JsonNode;
import com.jfl.appointment.policy.entity.PolicyCategory;
import com.jfl.appointment.policy.entity.PolicyStatus;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record ClinicPolicyResponse(

        Long id,
        Long clinicId,
        String policyCode,
        String policyName,
        PolicyCategory category,
        JsonNode policyConfig,
        PolicyStatus status,
        Integer version,
        Integer activeVersion,
        OffsetDateTime effectiveFrom,
        LocalDateTime updatedAt
) {
}
