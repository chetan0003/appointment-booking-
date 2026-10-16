package com.jfl.appointment.policy.service;


import com.fasterxml.jackson.databind.JsonNode;

import com.jfl.appointment.policy.dto.PolicyValidationResponse;
import com.jfl.appointment.policy.entity.PolicyCategory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ClinicPolicyValidator {

    public PolicyValidationResponse validate(
            PolicyCategory category,
            JsonNode config) {

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (config == null || !config.isObject()) {
            errors.add("Policy configuration must be a JSON object.");

            return new PolicyValidationResponse(
                    false, errors, warnings
            );
        }

        switch (category) {
            case APPOINTMENT ->
                    validateAppointment(config, errors, warnings);

            case PAYMENT ->
                    validatePayment(config, errors, warnings);

            case QUEUE ->
                    validateQueue(config, errors, warnings);

            case PATIENT_REGISTRATION ->
                    validatePatientRegistration(config, errors);

            case COMMUNICATION ->
                    validateCommunication(config, errors);

            case WORKING_HOURS ->
                    validateWorkingHours(config, errors);
        }

        return new PolicyValidationResponse(
                errors.isEmpty(),
                errors,
                warnings
        );
    }

    private void validateAppointment(
            JsonNode config,
            List<String> errors,
            List<String> warnings) {

        String bookingMode =
                config.path("bookingMode").asText("");

        if (!List.of("ONLINE_ONLY", "STAFF_ONLY", "BOTH")
                .contains(bookingMode)) {
            errors.add("Invalid bookingMode.");
        }

        int advanceDays =
                config.path("advanceBookingDays").asInt(-1);

        if (advanceDays < 1 || advanceDays > 365) {
            errors.add(
                    "advanceBookingDays must be between 1 and 365."
            );
        }

        int notice =
                config.path("minimumNoticeMinutes").asInt(-1);

        if (notice < 0 || notice > 10080) {
            errors.add(
                    "minimumNoticeMinutes must be between 0 and 10080."
            );
        }

        int duration =
                config.path("defaultAppointmentDurationMinutes").asInt(-1);

        if (duration < 5 || duration > 240) {
            errors.add(
                    "Appointment duration must be between 5 and 240 minutes."
            );
        }

        int interval =
                config.path("slotIntervalMinutes").asInt(-1);

        if (interval < 5 || interval > 240) {
            errors.add("Invalid slot interval.");
        }

        int buffer =
                config.path("bufferMinutes").asInt(-1);

        if (buffer < 0 || buffer > 120) {
            errors.add("Invalid buffer duration.");
        }

        if (interval > 0 && duration > 0 && interval > duration) {
            warnings.add(
                    "Slot interval exceeds appointment duration."
            );
        }

        if (config.path("allowCancellation").asBoolean(false)) {
            int cutoff =
                    config.path("cancellationCutoffHours").asInt(-1);

            if (cutoff < 0 || cutoff > 168) {
                errors.add("Invalid cancellation cutoff.");
            }
        }

        if (config.path("allowRescheduling").asBoolean(false)) {
            int cutoff =
                    config.path("rescheduleCutoffHours").asInt(-1);

            if (cutoff < 0 || cutoff > 168) {
                errors.add("Invalid reschedule cutoff.");
            }
        }
    }

    private void validatePayment(
            JsonNode config,
            List<String> errors,
            List<String> warnings) {

        String mode = config.path("paymentMode").asText("");

        if (!List.of(
                "BEFORE_QUEUE",
                "PARALLEL_WITH_QUEUE",
                "BEFORE_CONSULTATION"
        ).contains(mode)) {

            errors.add("Invalid paymentMode.");
        }

        String method =
                config.path("defaultPaymentMethod").asText("");

        if (!List.of("CASH", "ONLINE", "UPI", "CARD", "ANY")
                .contains(method)) {
            errors.add("Invalid defaultPaymentMethod.");
        }

        if (config.path("requireAdvancePayment").asBoolean(false)) {
            double percentage =
                    config.path("advancePaymentPercentage").asDouble(-1);

            if (percentage <= 0 || percentage > 100) {
                errors.add(
                        "Advance payment percentage must be between 1 and 100."
                );
            }
        }
    }

    private void validateQueue(
            JsonNode config,
            List<String> errors,
            List<String> warnings) {

        int maxWait =
                config.path("maxEstimatedWaitMinutes").asInt(-1);

        if (maxWait < 0 || maxWait > 1440) {
            errors.add("Invalid maximum estimated wait.");
        }

        int grace =
                config.path("patientArrivalGraceMinutes").asInt(-1);

        if (grace < 0 || grace > 180) {
            errors.add("Invalid patient arrival grace period.");
        }

        String paymentMode =
                config.path("paymentMode").asText("");

        if (!List.of(
                "BEFORE_QUEUE",
                "PARALLEL_WITH_QUEUE",
                "BEFORE_CONSULTATION"
        ).contains(paymentMode)) {
            errors.add("Invalid queue paymentMode.");
        }
    }

    private void validatePatientRegistration(
            JsonNode config,
            List<String> errors) {

        if (!config.has("requireMobileNumber")) {
            errors.add("requireMobileNumber is required.");
        }

        if (!config.has("allowDuplicateMobile")) {
            errors.add("allowDuplicateMobile is required.");
        }
    }

    private void validateCommunication(
            JsonNode config,
            List<String> errors) {

        String channel = config.path("defaultChannel").asText("");

        if (!List.of("WHATSAPP", "SMS", "BOTH", "NONE")
                .contains(channel)) {
            errors.add("Invalid communication channel.");
        }
    }

    private void validateWorkingHours(
            JsonNode config,
            List<String> errors) {

        JsonNode days = config.path("days");

        if (!days.isArray() || days.isEmpty()) {
            errors.add("Working hours must contain day configurations.");
            return;
        }

        for (JsonNode day : days) {
            boolean enabled = day.path("enabled").asBoolean(false);

            if (!enabled) {
                continue;
            }

            String start = day.path("startTime").asText("");
            String end = day.path("endTime").asText("");

            if (start.isBlank() || end.isBlank()) {
                errors.add(
                        "Enabled working days require startTime and endTime."
                );
            } else if (start.compareTo(end) >= 0) {
                errors.add(
                        "Working day endTime must be after startTime."
                );
            }
        }
    }
}
