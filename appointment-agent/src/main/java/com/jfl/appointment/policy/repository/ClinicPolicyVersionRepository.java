package com.jfl.appointment.policy.repository;


import com.jfl.appointment.policy.entity.ClinicPolicyVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClinicPolicyVersionRepository
        extends JpaRepository<ClinicPolicyVersion, Long> {

    List<ClinicPolicyVersion>
    findAllByClinicPolicyIdAndClinicIdOrderByVersionNumberDesc(
            Long clinicPolicyId,
            Long clinicId
    );

    Optional<ClinicPolicyVersion>
    findByClinicPolicyIdAndClinicIdAndVersionNumber(
            Long clinicPolicyId,
            Long clinicId,
            Integer versionNumber
    );

    boolean existsByClinicPolicyIdAndVersionNumber(
            Long clinicPolicyId,
            Integer versionNumber
    );
}
