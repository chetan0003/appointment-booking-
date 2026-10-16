package com.jfl.appointment.policy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.jfl.appointment.policy.entity.PolicyCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PaymentPolicyService {

    private final PolicyCacheService policyCacheService;



    private void validatePayment(
            Long clinicId,
            List<String> errors) {

        JsonNode config = policyCacheService.getAppointmentPolicy(clinicId, PolicyCategory.QUEUE);
        if (!config.isObject()) {
            errors.add(
                    "Payment configuration must be a JSON object."
            );
            return;
        }

        // Boolean fields
        validateRequiredBoolean(config, "allowCash", errors);
        validateRequiredBoolean(config, "allowRefund", errors);
        validateRequiredBoolean(config, "allowOnlinePayment", errors);
        validateRequiredBoolean(config, "allowPartialPayment", errors);
        validateRequiredBoolean(config, "requireAdvancePayment", errors);

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

        // Default payment method
        validateRequiredEnum(
                config,
                "defaultPaymentMethod",
                List.of(
                        "CASH",
                        "ONLINE",
                        "ANY"
                ),
                errors
        );

        // Refund window: 0–720 hours (30 days)
        validateRequiredIntegerRange(
                config,
                "refundWithinHours",
                0,
                720,
                errors
        );

        // Advance payment percentage: 0–100
        validateRequiredIntegerRange(
                config,
                "advancePaymentPercentage",
                0,
                100,
                errors
        );

        // Cross-field validation
        boolean allowCash = config.path("allowCash").asBoolean(false);
        boolean allowOnlinePayment = config.path("allowOnlinePayment").asBoolean(false);
        boolean requireAdvancePayment = config.path("requireAdvancePayment").asBoolean(false);
        int advancePercentage = config.path("advancePaymentPercentage").asInt(-1);

        if (!allowCash && !allowOnlinePayment) {
            errors.add(
                    "At least one payment method must be enabled."
            );
        }

        if (requireAdvancePayment
                && advancePercentage <= 0
                && config.path("advancePaymentPercentage").isIntegralNumber()) {
            errors.add(
                    "Advance payment percentage must be greater than 0 when advance payment is required."
            );
        }

        if (!requireAdvancePayment
                && advancePercentage > 0
                && advancePercentage <= 100) {
            errors.add(
                    "Advance payment percentage must be 0 when advance payment is disabled."
            );
        }

        if ("CASH".equalsIgnoreCase(
                config.path("defaultPaymentMethod").asText())
                && !allowCash) {
            errors.add(
                    "Default payment method CASH requires allowCash to be true."
            );
        }

        if ("ONLINE".equalsIgnoreCase(
                config.path("defaultPaymentMethod").asText())
                && !allowOnlinePayment) {
            errors.add(
                    "Default payment method ONLINE requires allowOnlinePayment to be true."
            );
        }
    }

    private void validateRequiredBoolean(
            JsonNode config,
            String field,
            List<String> errors) {

        if (!config.path(field).isBoolean()) {
            errors.add(field + " must be a boolean.");
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

        String normalized = value.asText()
                .trim()
                .toUpperCase(Locale.ROOT);

        if (!allowedValues.contains(normalized)) {
            errors.add(
                    "Invalid " + field + ". Allowed values: "
                            + String.join(", ", allowedValues) + "."
            );
        }
    }
}
