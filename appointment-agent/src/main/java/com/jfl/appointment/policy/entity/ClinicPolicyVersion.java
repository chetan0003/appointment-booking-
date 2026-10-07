package com.jfl.appointment.policy.entity;


import com.fasterxml.jackson.databind.JsonNode;

import com.jfl.appointment.entity.AuditableEntity;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "clinic_policy_version",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_policy_version",
                        columnNames = {
                                "clinic_policy_id",
                                "version_number"
                        }
                )
        }
)
@Getter
@Setter
public class ClinicPolicyVersion extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clinic_policy_id", nullable = false)
    private Long clinicPolicyId;

    @Column(name = "clinic_id", nullable = false)
    private Long clinicId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "policy_config",
            nullable = false,
            columnDefinition = "jsonb"
    )
    private JsonNode policyConfig;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyStatus status;

    @Column(name = "change_reason")
    private String changeReason;

    @Column(name = "activated_by")
    private String activatedBy;

    @Column(name = "activated_at", nullable = false)
    private OffsetDateTime activatedAt;
}
