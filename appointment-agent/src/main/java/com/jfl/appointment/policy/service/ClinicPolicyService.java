package com.jfl.appointment.policy.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.policy.dto.ClinicPolicyRequest;
import com.jfl.appointment.policy.dto.ClinicPolicyResponse;
import com.jfl.appointment.policy.dto.PolicyValidationResponse;
import com.jfl.appointment.policy.dto.PolicyVersionResponse;
import com.jfl.appointment.policy.entity.ClinicPolicy;
import com.jfl.appointment.policy.entity.ClinicPolicyVersion;
import com.jfl.appointment.policy.entity.PolicyCategory;
import com.jfl.appointment.policy.entity.PolicyStatus;
import com.jfl.appointment.policy.repository.ClinicPolicyRepository;
import com.jfl.appointment.policy.repository.ClinicPolicyVersionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClinicPolicyService {

    private final ClinicPolicyRepository policyRepository;

    private final ClinicPolicyVersionRepository versionRepository;

    private final ClinicPolicyValidator validator;

    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ClinicPolicyResponse> getPolicies(Long clinicId) {

        return policyRepository.findAllByClinicId(clinicId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClinicPolicyResponse> getPoliciesByCategory(
            Long clinicId,
            PolicyCategory category) {

        return policyRepository
                .findAllByClinicIdAndCategory(clinicId, category)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClinicPolicyResponse getPolicy(
            Long clinicId,
            Long policyId) {

        return toResponse(getEntity(clinicId, policyId));
    }

    public ClinicPolicyResponse createPolicy(
            Long clinicId,
            ClinicPolicyRequest request,
            String username) {

        if (policyRepository.existsByClinicIdAndPolicyCode(
                clinicId, request.policyCode())) {

            throw new IllegalArgumentException(
                    "Policy code already exists for this clinic."
            );
        }

        PolicyValidationResponse validation =
                validator.validate(
                        request.category(),
                        request.policyConfig()
                );

        if (!validation.valid()) {
            throw new IllegalArgumentException(
                    String.join("; ", validation.errors())
            );
        }

        ClinicPolicy policy = new ClinicPolicy();

        policy.setClinicId(clinicId);
        policy.setPolicyCode(request.policyCode());
        policy.setPolicyName(request.policyName());
        policy.setCategory(request.category());
        policy.setPolicyConfig(
                request.policyConfig().deepCopy()
        );
        policy.setStatus(PolicyStatus.DRAFT);
        policy.setVersion(1);
        //policy.setCreatedBy(username);
        //policy.setUpdatedBy(username);

        return toResponse(policyRepository.save(policy));
    }

    public ClinicPolicyResponse updateDraft(
            Long clinicId,
            Long policyId,
            ClinicPolicyRequest request,
            String username) {

        ClinicPolicy policy = getEntity(clinicId, policyId);

        if (policy.getStatus() == PolicyStatus.INACTIVE) {
            throw new IllegalStateException(
                    "Inactive policy cannot be edited."
            );
        }

        if (!policy.getPolicyCode().equals(request.policyCode())) {
            throw new IllegalArgumentException(
                    "Policy code cannot be changed."
            );
        }

        PolicyValidationResponse validation =
                validator.validate(
                        request.category(),
                        request.policyConfig()
                );

        if (!validation.valid()) {
            throw new IllegalArgumentException(
                    String.join("; ", validation.errors())
            );
        }

        policy.setPolicyName(request.policyName());
        policy.setCategory(request.category());
        policy.setPolicyConfig(
                request.policyConfig().deepCopy()
        );
        //policy.setUpdatedBy(username);

        return toResponse(policyRepository.save(policy));
    }

    public PolicyValidationResponse validatePolicy(
            Long clinicId,
            Long policyId) {

        ClinicPolicy policy = getEntity(clinicId, policyId);

        return validator.validate(
                policy.getCategory(),
                policy.getPolicyConfig()
        );
    }

    public ClinicPolicyResponse activatePolicy(
            Long clinicId,
            Long policyId,
            String username,
            String reason) {

        ClinicPolicy policy = getEntity(clinicId, policyId);

        PolicyValidationResponse validation =
                validator.validate(
                        policy.getCategory(),
                        policy.getPolicyConfig()
                );

        if (!validation.valid()) {
            throw new IllegalStateException(
                    "Policy validation failed: "
                            + String.join("; ", validation.errors())
            );
        }

        int nextVersion = versionRepository
                .findAllByClinicPolicyIdAndClinicIdOrderByVersionNumberDesc(
                        policyId, clinicId
                )
                .stream()
                .mapToInt(ClinicPolicyVersion::getVersionNumber)
                .max()
                .orElse(0) + 1;

        ClinicPolicyVersion snapshot =
                new ClinicPolicyVersion();

        snapshot.setClinicPolicyId(policy.getId());
        snapshot.setClinicId(clinicId);
        snapshot.setVersionNumber(nextVersion);
        snapshot.setPolicyConfig(
                policy.getPolicyConfig().deepCopy()
        );
        snapshot.setStatus(PolicyStatus.ACTIVE);
        snapshot.setChangeReason(reason);
        snapshot.setActivatedBy(username);
        snapshot.setActivatedAt(OffsetDateTime.now());

        versionRepository.save(snapshot);

        policy.setStatus(PolicyStatus.ACTIVE);
        policy.setActiveVersion(nextVersion);
        policy.setEffectiveFrom(OffsetDateTime.now());
        policy.setEffectiveTo(null);
        //policy.setUpdatedBy(username);

        return toResponse(policyRepository.save(policy));
    }

    @Transactional(readOnly = true)
    public List<PolicyVersionResponse> getVersionHistory(
            Long clinicId,
            Long policyId) {

        getEntity(clinicId, policyId);

        return versionRepository
                .findAllByClinicPolicyIdAndClinicIdOrderByVersionNumberDesc(
                        policyId, clinicId
                )
                .stream()
                .map(this::toVersionResponse)
                .toList();
    }

    public ClinicPolicyResponse rollback(
            Long clinicId,
            Long policyId,
            Integer targetVersion,
            String username) {

        ClinicPolicy policy = getEntity(clinicId, policyId);

        ClinicPolicyVersion target = versionRepository
                .findByClinicPolicyIdAndClinicIdAndVersionNumber(
                        policyId, clinicId, targetVersion
                )
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Policy version not found: " + targetVersion
                        )
                );

        policy.setPolicyConfig(
                target.getPolicyConfig().deepCopy()
        );

        policy.setStatus(PolicyStatus.DRAFT);
        //policy.setUpdatedBy(username);

        policyRepository.save(policy);

        // Rollback creates a new version upon activation.
        // It does not delete or modify historical snapshots.

        return toResponse(policy);
    }

    public void deactivatePolicy(
            Long clinicId,
            Long policyId,
            String username) {

        ClinicPolicy policy = getEntity(clinicId, policyId);

        policy.setStatus(PolicyStatus.INACTIVE);
        policy.setEffectiveTo(OffsetDateTime.now());
        //policy.setUpdatedBy(username);

        policyRepository.save(policy);
    }

    private ClinicPolicy getEntity(
            Long clinicId,
            Long policyId) {

        return policyRepository
                .findByIdAndClinicId(policyId, clinicId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Policy not found: " + policyId
                        )
                );
    }

    private ClinicPolicyResponse toResponse(
            ClinicPolicy policy) {

        return new ClinicPolicyResponse(
                policy.getId(),
                policy.getClinicId(),
                policy.getPolicyCode(),
                policy.getPolicyName(),
                policy.getCategory(),
                policy.getPolicyConfig(),
                policy.getStatus(),
                policy.getVersion(),
                policy.getActiveVersion(),
                policy.getEffectiveFrom(),
                policy.getUpdatedAt()
        );
    }

    private PolicyVersionResponse toVersionResponse(
            ClinicPolicyVersion version) {

        return new PolicyVersionResponse(
                version.getId(),
                version.getClinicPolicyId(),
                version.getVersionNumber(),
                version.getPolicyConfig(),
                version.getStatus(),
                version.getChangeReason(),
                version.getActivatedBy(),
                version.getActivatedAt()
        );
    }
}
