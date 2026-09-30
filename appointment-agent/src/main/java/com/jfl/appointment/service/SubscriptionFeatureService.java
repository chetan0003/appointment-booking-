package com.jfl.appointment.service;

import com.jfl.appointment.entity.*;
import com.jfl.appointment.exception.SubscriptionFeatureNotAvailableException;
import com.jfl.appointment.exception.SubscriptionLimitExceededException;
import com.jfl.appointment.exception.SubscriptionRequiredException;
import com.jfl.appointment.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionFeatureService {

    private final ClinicSubscriptionRepository subscriptionRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final ClinicUserRepository userRepository;
    private final PatientRepository patientRepository;

    public ClinicSubscription getActiveSubscription(Long clinicId) {

        ClinicSubscription subscription =
                subscriptionRepository.findByClinicId(clinicId)
                        .orElseThrow(() ->
                                new SubscriptionRequiredException(
                                        "No subscription found for clinic"
                                )
                        );

        if (!isActive(subscription)) {
            throw new SubscriptionRequiredException(
                    "Your subscription is not active"
            );
        }

        return subscription;
    }

    public void validateFeature(
            Long clinicId,
            SubscriptionFeature feature) {

        ClinicSubscription subscription =
                getActiveSubscription(clinicId);

        SubscriptionPlan plan = subscription.getPlan();

        switch (feature) {

            case APPOINTMENTS -> validateAppointments(
                    clinicId,
                    subscription,
                    plan
            );

            case DOCTORS -> validateDoctors(
                    clinicId,
                    plan
            );

            case STAFF -> validateStaff(
                    clinicId,
                    plan
            );

            case PATIENTS -> validatePatients(
                    clinicId,
                    plan
            );

            case AI_RECEPTIONIST -> validateAiReceptionist(plan);

            case WHATSAPP -> validateWhatsapp(plan);

            case ANALYTICS -> validateAnalytics(plan);
        }
    }

    public void validateFeatureOnWhats(
            Long clinicId) {

        ClinicSubscription subscription =
                getActiveSubscription(clinicId);

        SubscriptionPlan plan = subscription.getPlan();

        validateAppointments(
                clinicId,
                subscription,
                plan
        );

        validatePatients(
                clinicId,
                plan
        );
        validateAiReceptionist(plan);

    }

    private void validateAppointments(
            Long clinicId,
            ClinicSubscription subscription,
            SubscriptionPlan plan) {

        Integer limit = plan.getMaxAppointmentsPerMonth();

        // null or negative means unlimited
        if (limit == null || limit < 0) {
            return;
        }

        long used =
                appointmentRepository.countAppointmentsForPeriod(
                        clinicId,
                        subscription.getStartDate(),
                        subscription.getEndDate()
                );

        if (used >= limit) {
            throw new SubscriptionLimitExceededException(
                    "Appointment limit reached. Your "
                            + plan.getName()
                            + " plan allows "
                            + limit
                            + " appointments per subscription period."
            );
        }
    }

    private void validateDoctors(
            Long clinicId,
            SubscriptionPlan plan) {

        Integer limit = plan.getMaxDoctors();

        // null or negative means unlimited
        if (limit == null || limit < 0) {
            return;
        }

        long currentCount =
                doctorRepository.countByClinicId(clinicId);

        if (currentCount >= limit) {
            throw new SubscriptionLimitExceededException(
                    "Doctor limit reached. Your "
                            + plan.getName()
                            + " plan allows only "
                            + limit
                            + " doctors."
            );
        }
    }

    private void validateStaff(
            Long clinicId,
            SubscriptionPlan plan) {

        Integer limit = plan.getMaxStaff();

        // null or negative means unlimited
        if (limit == null || limit < 0) {
            return;
        }

        long currentCount =
                userRepository.countUsersByClinicAndRole(clinicId, "STAFF");

        if (currentCount >= limit) {
            throw new SubscriptionLimitExceededException(
                    "Staff limit reached. Your "
                            + plan.getName()
                            + " plan allows only "
                            + limit
                            + " staff members."
            );
        }
    }

    private void validatePatients(
            Long clinicId,
            SubscriptionPlan plan) {

        Integer limit = plan.getMaxPatients();

        // null or negative means unlimited
        if (limit == null || limit < 0) {
            return;
        }

        long currentCount =
                patientRepository.countByClinicId(clinicId);

        if (currentCount >= limit) {
            throw new SubscriptionLimitExceededException(
                    "Patient limit reached. Your "
                            + plan.getName()
                            + " plan allows only "
                            + limit
                            + " patients."
            );
        }
    }

    private void validateAiReceptionist(
            SubscriptionPlan plan) {

        if (!plan.isAiReceptionistEnabled()) {
            throw new SubscriptionFeatureNotAvailableException(
                    "AI Receptionist is not available in your "
                            + plan.getName()
                            + " plan."
            );
        }
    }

    private void validateWhatsapp(
            SubscriptionPlan plan) {

        if (!plan.isWhatsappEnabled()) {
            throw new SubscriptionFeatureNotAvailableException(
                    "WhatsApp integration is not available in your "
                            + plan.getName()
                            + " plan."
            );
        }
    }

    private void validateAnalytics(
            SubscriptionPlan plan) {

        if (!plan.isAnalyticsEnabled()) {
            throw new SubscriptionFeatureNotAvailableException(
                    "Analytics is not available in your "
                            + plan.getName()
                            + " plan."
            );
        }
    }

    private boolean isActive(
            ClinicSubscription subscription) {

        LocalDate today = LocalDate.now();

        if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {

            return subscription.getStartDate() != null
                    && subscription.getEndDate() != null
                    && !today.isBefore(
                    subscription.getStartDate()
            )
                    && !today.isAfter(
                    subscription.getEndDate()
            );
        }

        if (subscription.getStatus() == SubscriptionStatus.TRIALING) {

            return subscription.getTrialEndDate() != null
                    && !today.isAfter(
                    subscription.getTrialEndDate()
            );
        }

        return false;
    }
}