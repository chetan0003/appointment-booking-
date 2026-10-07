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
        name = "clinic_policy",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_clinic_policy_code",
                        columnNames = {"clinic_id", "policy_code"}
                )
        }
)
@Getter
@Setter
public class ClinicPolicy extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clinic_id", nullable = false)
    private Long clinicId;

    @Column(name = "policy_code", nullable = false)
    private String policyCode;

    @Column(name = "policy_name", nullable = false)
    private String policyName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyCategory category;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "policy_config",
            nullable = false,
            columnDefinition = "jsonb"
    )
    private JsonNode policyConfig;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyStatus status = PolicyStatus.DRAFT;

    @Version
    @Column(nullable = false)
    private Integer version = 1;

    @Column(name = "active_version")
    private Integer activeVersion;

    @Column(name = "effective_from")
    private OffsetDateTime effectiveFrom;

    @Column(name = "effective_to")
    private OffsetDateTime effectiveTo;

}