package com.jfl.appointment.policy.service;

import com.fasterxml.jackson.databind.JsonNode;

import com.jfl.appointment.exception.ForbiddenException;
import com.jfl.appointment.policy.entity.ClinicPolicy;
import com.jfl.appointment.policy.entity.ClinicPolicyVersion;
import com.jfl.appointment.policy.entity.PolicyCategory;
import com.jfl.appointment.policy.entity.PolicyStatus;
import com.jfl.appointment.policy.repository.ClinicPolicyRepository;
import com.jfl.appointment.policy.repository.ClinicPolicyVersionRepository;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClinicPolicyResolver {

    private final ClinicPolicyRepository policyRepository;

    private final ClinicPolicyVersionRepository versionRepository;

    public JsonNode getActivePolicy(
            Long clinicId,
            PolicyCategory category) {
        log.info("Get Active Policy For :: {}",category);
        ClinicPolicy policy = policyRepository
                .findAllByClinicIdAndCategory(clinicId, category)
                .stream()
                .filter(p -> p.getStatus() == PolicyStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() ->
                        new ForbiddenException(
                                "No active policy found for " + category
                        )
                );

        Integer activeVersion = policy.getActiveVersion();

        if (activeVersion == null) {
            throw new ForbiddenException(
                    "Active policy version is missing."
            );
        }

        ClinicPolicyVersion snapshot = versionRepository
                .findByClinicPolicyIdAndClinicIdAndVersionNumber(
                        policy.getId(),
                        clinicId,
                        activeVersion
                )
                .orElseThrow(() ->
                        new ForbiddenException(
                                "Active policy snapshot not found."
                        )
                );
        log.info("Get Active Policy For fetched successfully..");
        return snapshot.getPolicyConfig();
    }
}
