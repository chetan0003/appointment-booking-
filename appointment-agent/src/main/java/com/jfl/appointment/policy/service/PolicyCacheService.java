package com.jfl.appointment.policy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.jfl.appointment.policy.entity.PolicyCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PolicyCacheService {

    private final ClinicPolicyResolver policyResolver;

    @Cacheable(
            cacheNames = "appointmentPolicies",
            key = "#clinicId + '_' + #policyCategory"
    )
    public JsonNode getAppointmentPolicy(
            Long clinicId,
            PolicyCategory policyCategory) {

        return policyResolver.getActivePolicy(
                clinicId,
                policyCategory
        );
    }

    @CacheEvict(
            cacheNames = "appointmentPolicies",
            key = "#clinicId + '_' + #policyCategory"
    )
    public void evictPolicy(
            Long clinicId,
            PolicyCategory policyCategory) {

        // Cache eviction is performed by Spring.
    }
}
