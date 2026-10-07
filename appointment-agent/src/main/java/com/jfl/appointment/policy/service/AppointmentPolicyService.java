
package com.jfl.appointment.policy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.jfl.appointment.exception.BusinessException;
import com.jfl.appointment.exception.DailyBookingLimitException;
import com.jfl.appointment.exception.ForbiddenException;
import com.jfl.appointment.policy.entity.PolicyCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AppointmentPolicyService {

    private final ClinicPolicyResolver policyResolver;

    /**
     * CREATE APPOINTMENT VALIDATION
     */
    public void validateCreateBooking(
            Long clinicId,
            LocalDateTime appointmentDateTime,
            int existingAppointmentsToday) {

        JsonNode config = getAppointmentPolicy(clinicId);

        validateBookingMode(config, "ONLINE");

        validateMinimumNotice(
                config,
                appointmentDateTime
        );

        validateAdvanceBooking(
                config,
                appointmentDateTime
        );

        validateDailyLimit(
                config,
                existingAppointmentsToday
        );
    }

    /**
     * WALK-IN APPOINTMENT VALIDATION
     */
    public void validateWalkInBooking(
            Long clinicId,
            LocalDateTime appointmentDateTime,
            int existingAppointmentsToday) {

        JsonNode config = getAppointmentPolicy(clinicId);

        if (!config.path("allowWalkIns").asBoolean(false)) {
            throw new BusinessException(
                    "Walk-in appointments are not allowed."
            );
        }

        validateBookingMode(config, "WALK_IN");

        validateDailyLimit(
                config,
                existingAppointmentsToday
        );
    }

    /**
     * RESCHEDULE APPOINTMENT VALIDATION
     *
     * existingAppointmentDateTime = current appointment time
     * newAppointmentDateTime = requested new time
     */
    public void validateReschedule(
            Long clinicId,
            LocalDateTime existingAppointmentDateTime,
            LocalDateTime newAppointmentDateTime,
            int existingAppointmentsOnNewDate) {

        JsonNode config = getAppointmentPolicy(clinicId);

        if (!config.path("allowRescheduling").asBoolean(false)) {
            throw new BusinessException(
                    "Appointment rescheduling is not allowed."
            );
        }

        int cutoffHours =
                config.path("rescheduleCutoffHours").asInt(0);

        LocalDateTime cutoffTime =
                existingAppointmentDateTime.minusHours(cutoffHours);

        if (LocalDateTime.now().isAfter(cutoffTime)) {
            throw new BusinessException(
                    "Rescheduling is allowed only "
                            + cutoffHours
                            + " hours before the appointment."
            );
        }

        validateMinimumNotice(
                config,
                newAppointmentDateTime
        );

        validateAdvanceBooking(
                config,
                newAppointmentDateTime
        );

        validateDailyLimit(
                config,
                existingAppointmentsOnNewDate
        );
    }

    /**
     * CANCEL APPOINTMENT VALIDATION
     */
    public void validateCancellation(
            Long clinicId,
            LocalDateTime appointmentDateTime) {

        JsonNode config = getAppointmentPolicy(clinicId);

        if (!config.path("allowCancellation").asBoolean(false)) {
            throw new BusinessException(
                    "Appointment cancellation is not allowed."
            );
        }

        int cutoffHours =
                config.path("cancellationCutoffHours").asInt(0);

        LocalDateTime cutoffTime =
                appointmentDateTime.minusHours(cutoffHours);

        if (LocalDateTime.now().isAfter(cutoffTime)) {
            throw new BusinessException(
                    "Cancellation is allowed only "
                            + cutoffHours
                            + " hours before the appointment."
            );
        }
    }

    /**
     * FOLLOW-UP APPOINTMENT VALIDATION
     */
    public void validateFollowUp(
            Long clinicId,
            LocalDateTime followUpDateTime,
            int existingAppointmentsToday) {

        JsonNode config = getAppointmentPolicy(clinicId);

        validateMinimumNotice(
                config,
                followUpDateTime
        );

        validateAdvanceBooking(
                config,
                followUpDateTime
        );

        validateDailyLimit(
                config,
                existingAppointmentsToday
        );
    }

    /**
     * NO-SHOW VALIDATION
     */
    public void validateNoShow(
            Long clinicId,
            LocalDateTime appointmentDateTime) {

        JsonNode config = getAppointmentPolicy(clinicId);

        int noShowAfterMinutes =
                config.path("noShowAfterMinutes").asInt(15);

        LocalDateTime noShowEligibleTime =
                appointmentDateTime.plusMinutes(noShowAfterMinutes);

        if (LocalDateTime.now().isBefore(noShowEligibleTime)) {
            throw new ForbiddenException(
                    "Appointment cannot be marked as no-show before "
                            + noShowAfterMinutes
                            + " minutes after scheduled time."
            );
        }
    }

    /**
     * COMMON VALIDATION METHODS
     */

    private JsonNode getAppointmentPolicy(Long clinicId) {

        return policyResolver.getActivePolicy(
                clinicId,
                PolicyCategory.APPOINTMENT
        );
    }

    private void validateBookingMode(
            JsonNode config,
            String requestedMode) {

        String bookingMode =
                config.path("bookingMode").asText("ONLINE");

        if ("BOTH".equalsIgnoreCase(bookingMode)) {
            return;
        }

        if (!bookingMode.equalsIgnoreCase(requestedMode)) {
            throw new DailyBookingLimitException(
                    requestedMode
                            + " booking is not allowed by clinic policy."
            );
        }
    }

    private void validateMinimumNotice(
            JsonNode config,
            LocalDateTime appointmentDateTime) {

        int minimumNoticeMinutes =
                config.path("minimumNoticeMinutes").asInt(0);

        LocalDateTime minimumAllowedTime =
                LocalDateTime.now().plusMinutes(minimumNoticeMinutes);

        if (appointmentDateTime.isBefore(minimumAllowedTime)) {
            throw new DailyBookingLimitException(
                    "Appointment must be booked at least "
                            + minimumNoticeMinutes
                            + " minutes in advance."
            );
        }
    }

    private void validateAdvanceBooking(
            JsonNode config,
            LocalDateTime appointmentDateTime) {

        int advanceBookingDays =
                config.path("advanceBookingDays").asInt(30);

        LocalDate maxAllowedDate =
                LocalDate.now().plusDays(advanceBookingDays);

        if (appointmentDateTime.toLocalDate()
                .isAfter(maxAllowedDate)) {

            throw new DailyBookingLimitException(
                    "Appointment cannot be booked more than "
                            + advanceBookingDays
                            + " days in advance."
            );
        }
    }

    private void validateDailyLimit(
            JsonNode config,
            int existingAppointmentsToday) {

        int maxAppointments =
                config.path("maxAppointmentsPerPatientPerDay").asInt(1);

        if (existingAppointmentsToday >= maxAppointments) {
            throw new DailyBookingLimitException(
                    "Maximum appointments per patient per day reached."
            );
        }
    }
}
