package com.jfl.appointment.policy.repository;



import com.jfl.appointment.policy.entity.ClinicPolicy;
import com.jfl.appointment.policy.entity.PolicyCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClinicPolicyRepository
        extends JpaRepository<ClinicPolicy, Long> {

    List<ClinicPolicy> findAllByClinicId(Long clinicId);

    List<ClinicPolicy> findAllByClinicIdAndCategory(
            Long clinicId,
            PolicyCategory category
    );

    Optional<ClinicPolicy> findByIdAndClinicId(
            Long id,
            Long clinicId
    );

    Optional<ClinicPolicy> findByClinicIdAndPolicyCode(
            Long clinicId,
            String policyCode
    );

    boolean existsByClinicIdAndPolicyCode(
            Long clinicId,
            String policyCode
    );
}
