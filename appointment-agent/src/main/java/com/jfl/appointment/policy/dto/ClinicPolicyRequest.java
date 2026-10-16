package com.jfl.appointment.policy.dto;


import com.fasterxml.jackson.databind.JsonNode;

import com.jfl.appointment.policy.entity.PolicyCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClinicPolicyRequest(

        @NotBlank
        @Size(max = 50)
        String policyCode,

        @NotBlank
        @Size(max = 150)
        String policyName,

        @NotNull
        PolicyCategory category,

        @NotNull
        JsonNode policyConfig,

        @Size(max = 500)
        String changeReason
) {
}
