package com.jfl.appointment.policy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.jfl.appointment.policy.entity.PolicyCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class QueuePolicyService {

    private final PolicyCacheService policyCacheService;

    private void validateQueue(
            Long clinicId,
            List<String> errors) {

        JsonNode config = policyCacheService.getAppointmentPolicy(clinicId, PolicyCategory.QUEUE);
        if (!config.isObject()) {
            errors.add("Queue policy configuration must be a JSON object.");
            return;
        }

        // Required boolean
        validateRequiredBoolean(config, "enabled", errors);

        // Queue mode
        validateRequiredEnum(
                config,
                "queueMode",
                List.of("TOKEN_BASED"),
                errors
        );

        // Maximum estimated waiting time: 0–1440 minutes
        validateRequiredIntegerRange(
                config,
                "maxEstimatedWaitMinutes",
                0,
                1440,
                errors
        );

        // Patient arrival grace period: 0–180 minutes
        validateRequiredIntegerRange(
                config,
                "patientArrivalGraceMinutes",
                0,
                180,
                errors
        );

        // Payment mode
        validateRequiredEnum(
                config,
                "paymentMode",
                List.of(
                        "BEFORE_QUEUE",
                        "PARALLEL_WITH_QUEUE",
                        "BEFORE_CONSULTATION"
                ),
                errors
        );

        // Token generation configuration
        JsonNode tokenGeneration = config.path("tokenGeneration");

        if (tokenGeneration.isMissingNode()
                || !tokenGeneration.isObject()) {
            errors.add(
                    "Queue tokenGeneration configuration is required."
            );
        } else {
            validateRequiredBoolean(
                    tokenGeneration,
                    "enabled",
                    errors
            );

            validateRequiredString(
                    tokenGeneration,
                    "prefix",
                    errors
            );

            validateRequiredEnum(
                    tokenGeneration,
                    "resetFrequency",
                    List.of("DAILY", "WEEKLY", "MONTHLY", "NEVER"),
                    errors
            );

            validateRequiredIntegerRange(
                    tokenGeneration,
                    "startNumber",
                    1,
                    Integer.MAX_VALUE,
                    errors
            );
        }

        // Queue assignment configuration
        JsonNode queueAssignment = config.path("queueAssignment");

        if (queueAssignment.isMissingNode()
                || !queueAssignment.isObject()) {
            errors.add(
                    "Queue queueAssignment configuration is required."
            );
        } else {
            validateRequiredBoolean(
                    queueAssignment,
                    "automatic",
                    errors
            );

            validateRequiredBoolean(
                    queueAssignment,
                    "assignOnCheckIn",
                    errors
            );

            validateRequiredBoolean(
                    queueAssignment,
                    "allowManualAssignment",
                    errors
            );
        }

        // Consultation duration
        validateRequiredIntegerRange(
                config,
                "defaultConsultationMinutes",
                1,
                240,
                errors
        );
    }

    private void validateRequiredBoolean(
            JsonNode config,
            String field,
            List<String> errors) {

        JsonNode value = config.path(field);

        if (!value.isBoolean()) {
            errors.add(
                    field + " must be a boolean (true or false)."
            );
        }
    }

    private void validateRequiredString(
            JsonNode config,
            String field,
            List<String> errors) {

        JsonNode value = config.path(field);

        if (!value.isTextual() || value.asText().isBlank()) {
            errors.add(field + " is required and must be a string.");
        }
    }

    private void validateRequiredIntegerRange(
            JsonNode config,
            String field,
            int min,
            int max,
            List<String> errors) {

        JsonNode value = config.path(field);

        if (!value.isIntegralNumber() || !value.canConvertToInt()) {
            errors.add(field + " must be an integer.");
            return;
        }

        int number = value.intValue();

        if (number < min || number > max) {
            errors.add(
                    field + " must be between "
                            + min + " and " + max + "."
            );
        }
    }

    private void validateRequiredEnum(
            JsonNode config,
            String field,
            List<String> allowedValues,
            List<String> errors) {

        JsonNode value = config.path(field);

        if (!value.isTextual() || value.asText().isBlank()) {
            errors.add(field + " is required.");
            return;
        }

        String normalizedValue = value.asText()
                .trim()
                .toUpperCase(Locale.ROOT);

        if (!allowedValues.contains(normalizedValue)) {
            errors.add(
                    "Invalid " + field + ". Allowed values: "
                            + String.join(", ", allowedValues) + "."
            );
        }
    }

}
