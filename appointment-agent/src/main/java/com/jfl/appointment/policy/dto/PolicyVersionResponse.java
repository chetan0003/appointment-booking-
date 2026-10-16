package com.jfl.appointment.policy.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.jfl.appointment.policy.entity.PolicyStatus;


import java.time.OffsetDateTime;

public record PolicyVersionResponse(

        Long id,
        Long clinicPolicyId,
        Integer versionNumber,
        JsonNode policyConfig,
        PolicyStatus status,
        String changeReason,
        String activatedBy,
        OffsetDateTime activatedAt
) {
}
