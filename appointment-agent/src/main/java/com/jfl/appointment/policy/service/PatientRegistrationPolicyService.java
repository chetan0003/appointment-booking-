package com.jfl.appointment.policy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.jfl.appointment.policy.entity.PolicyCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PatientRegistrationPolicyService {

    private final PolicyCacheService policyCacheService;
    private void validatePatientRegistration(
            Long  clinicId,
            List<String> errors) {

        JsonNode config = policyCacheService.getAppointmentPolicy(clinicId, PolicyCategory.PATIENT_REGISTRATION);
        if (!config.isObject()) {
            errors.add(
                    "Patient registration configuration must be a JSON object."
            );
            return;
        }

        // Required fields
        validateRequiredBoolean(config, "enabled", errors);
        validateRequiredBoolean(config, "requireMobileNumber", errors);
        validateRequiredBoolean(config, "allowDuplicateMobile", errors);

        // Registration number configuration
        JsonNode registrationNumber = config.path("registrationNumber");

        if (registrationNumber.isMissingNode()
                || !registrationNumber.isObject()) {
            errors.add(
                    "Registration number configuration is required."
            );
        } else {
            validateRequiredBoolean(
                    registrationNumber,
                    "autoGenerate",
                    errors
            );

            validateRequiredString(
                    registrationNumber,
                    "prefix",
                    errors
            );

            validateRequiredEnum(
                    registrationNumber,
                    "resetFrequency",
                    List.of("NEVER", "DAILY", "MONTHLY", "YEARLY"),
                    errors
            );

            validateRequiredBoolean(
                    registrationNumber,
                    "allowManualOverride",
                    errors
            );
        }

        // Required patient fields
        validateRequiredStringArray(
                config,
                "requiredFields",
                errors
        );

        // Optional patient fields, if supplied
        if (config.has("optionalFields")) {
            validateStringArray(
                    config,
                    "optionalFields",
                    errors
            );
        }

        // Mobile number configuration, if supplied
        JsonNode mobileNumber = config.path("mobileNumber");

        if (!mobileNumber.isMissingNode()) {
            if (!mobileNumber.isObject()) {
                errors.add(
                        "mobileNumber configuration must be an object."
                );
            } else {
                validateRequiredBoolean(
                        mobileNumber,
                        "countryCodeRequired",
                        errors
                );

                validateRequiredBoolean(
                        mobileNumber,
                        "validateFormat",
                        errors
                );
            }
        }

        // Email configuration, if supplied
        JsonNode email = config.path("email");

        if (!email.isMissingNode()) {
            if (!email.isObject()) {
                errors.add(
                        "email configuration must be an object."
                );
            } else {
                validateRequiredBoolean(
                        email,
                        "required",
                        errors
                );

                validateRequiredBoolean(
                        email,
                        "validateFormat",
                        errors
                );
            }
        }

        // Date of birth configuration, if supplied
        JsonNode dateOfBirth = config.path("dateOfBirth");

        if (!dateOfBirth.isMissingNode()) {
            if (!dateOfBirth.isObject()) {
                errors.add(
                        "dateOfBirth configuration must be an object."
                );
            } else {
                validateRequiredBoolean(
                        dateOfBirth,
                        "required",
                        errors
                );

                validateRequiredBoolean(
                        dateOfBirth,
                        "allowFutureDate",
                        errors
                );
            }
        }

        // Consent configuration, if supplied
        JsonNode consent = config.path("consent");

        if (!consent.isMissingNode()) {
            if (!consent.isObject()) {
                errors.add(
                        "consent configuration must be an object."
                );
            } else {
                validateRequiredBoolean(
                        consent,
                        "requirePrivacyConsent",
                        errors
                );

                validateRequiredBoolean(
                        consent,
                        "requireCommunicationConsent",
                        errors
                );

                validateRequiredBoolean(
                        consent,
                        "storeConsentTimestamp",
                        errors
                );
            }
        }
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

    private void validateRequiredStringArray(
            JsonNode config,
            String field,
            List<String> errors) {

        JsonNode array = config.path(field);

        if (!array.isArray() || array.isEmpty()) {
            errors.add(field + " must contain at least one value.");
            return;
        }

        validateStringArray(config, field, errors);
    }

    private void validateStringArray(
            JsonNode config,
            String field,
            List<String> errors) {

        JsonNode array = config.path(field);

        if (!array.isArray()) {
            errors.add(field + " must be an array.");
            return;
        }

        for (JsonNode item : array) {
            if (!item.isTextual() || item.asText().isBlank()) {
                errors.add(
                        field + " must contain only non-empty strings."
                );
                return;
            }
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
