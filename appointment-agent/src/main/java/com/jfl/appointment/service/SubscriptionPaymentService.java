package com.jfl.appointment.service;



import com.jfl.appointment.dto.PaymentResponse;
import com.jfl.appointment.dto.SubmitPaymentRequest;
import com.jfl.appointment.dto.VerifyPaymentRequest;
import com.jfl.appointment.entity.*;
import com.jfl.appointment.exception.ConflictException;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.repository.*;

import com.jfl.appointment.security.SecurityContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionPaymentService {

    private final SubscriptionPaymentRepository paymentRepository;
    private final SubscriptionPlanRepository planRepository;
    private final ClinicSubscriptionRepository subscriptionRepository;
    private final ClinicRepository clinicRepository;
    private final ClinicSubscriptionService subscriptionService;
    private final EmailService emailService;
    private final SecurityContextService securityContextService;
    private final AppUserRepository appUserRepository;
    private final ClinicUserRepository clinicUserRepository;

    /**
     * Clinic submits UPI transaction ID / UTR.
     */
    public PaymentResponse submitPayment(
            Long clinicId,
            SubmitPaymentRequest request
    ) {

        Clinic clinic =
                clinicRepository.findById(clinicId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Clinic not found: "
                                                + clinicId
                                ));

        SubscriptionPlan plan =
                planRepository.findById(
                                request.planId()
                        )
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Subscription plan not found: "
                                                + request.planId()
                                ));

        if (!plan.isActive()) {
            throw new IllegalStateException(
                    "Selected subscription plan is not active."
            );
        }

        String transactionId =
                request.transactionId().trim();

        if (paymentRepository
                .existsByTransactionId(transactionId)) {

            throw new ConflictException(
                    "Transaction ID has already been submitted."
            );
        }

        /*
         * Find existing subscription.
         *
         * If clinic doesn't have one yet,
         * create PENDING subscription.
         */
        ClinicSubscription subscription =
                subscriptionRepository
                        .findByClinicId(clinicId)
                        .orElseGet(() -> {

                            ClinicSubscription newSubscription =
                                    new ClinicSubscription();

                            newSubscription.setClinic(clinic);
                            newSubscription.setPlan(plan);
                            newSubscription.setStatus(
                                    SubscriptionStatus.PENDING
                            );
                            newSubscription.setAutoRenew(false);

                            return subscriptionRepository.save(
                                    newSubscription
                            );
                        });

        /*
         * If subscription already exists,
         * update selected plan.
         */
        subscription.setPlan(plan);

        /*
         * Don't allow another payment while
         * previous payment is still pending.
         */
        boolean pendingPaymentExists =
                paymentRepository
                        .existsBySubscriptionIdAndStatus(
                                subscription.getId(),
                                SubscriptionPaymentStatus.PENDING
                        );

        if (pendingPaymentExists) {

            throw new IllegalStateException(
                    "A payment is already pending verification."
            );
        }

        SubscriptionPayment payment =
                new SubscriptionPayment();

        payment.setSubscription(subscription);

        payment.setTransactionId(
                transactionId
        );

        /*
         * IMPORTANT:
         * Amount comes from the backend plan.
         */
        payment.setAmount(
                plan.getMonthlyPrice()
        );

        payment.setCurrency("INR");

        payment.setStatus(
                SubscriptionPaymentStatus.PENDING
        );

        payment.setPaymentDate(
                LocalDateTime.now()
        );

        SubscriptionPayment saved =
                paymentRepository.save(payment);

        //send email to admin
        Optional<AppUser> superadmin = appUserRepository.findByUsername("superadmin");
        Long createdBy = payment.getCreatedBy();
        Optional<AppUser> byId = appUserRepository.findById(createdBy);
        AppUser appUser = superadmin.get();
        emailService.sendSubscriptionPaymentSubmittedEmail(
                appUser.getEmail(),
                appUser.getFirstName(),
                byId.get().getFirstName(),
                clinic.getName(),
                plan.getName(),
                request.transactionId(),
                payment.getAmount());
        return toResponse(saved);
    }

    /**
     * SUPER_ADMIN verifies payment.
     */
    public PaymentResponse verifyPayment(
            Long paymentId,
            VerifyPaymentRequest request
    ) {

        SubscriptionPayment payment =
                paymentRepository.findById(paymentId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Payment not found: "
                                                + paymentId
                                ));

        if (payment.getStatus()
                != SubscriptionPaymentStatus.PENDING) {

            throw new IllegalStateException(
                    "Payment has already been processed."
            );
        }

        ClinicSubscription subscription =
                payment.getSubscription();

        if (Boolean.TRUE.equals(
                request.approved()
        )) {

            /*
             * PAYMENT VERIFIED
             */
            payment.setStatus(
                    SubscriptionPaymentStatus.VERIFIED
            );

            payment.setVerifiedAt(
                    LocalDateTime.now()
            );

            payment.setRejectionReason(null);

            /*
             * ACTIVATE SUBSCRIPTION
             */
            subscriptionService.activateSubscription(
                    subscription
            );
            Long paymentSubmitterId = payment.getCreatedBy();

            appUserRepository.findById(paymentSubmitterId)
                    .ifPresentOrElse(
                            appUser -> emailService.sendSubscriptionActivatedEmail(
                                    appUser.getEmail(),
                                    appUser.getFirstName(),
                                    subscription.getClinic().getName(),
                                    subscription.getPlan().getName(),
                                    LocalDate.now(),
                                    LocalDate.now().plusDays(30)
                            ),
                            () -> log.warn("AppUser not found for id: {}", paymentSubmitterId)
                    );
        } else {

            /*
             * PAYMENT REJECTED
             */
            payment.setStatus(
                    SubscriptionPaymentStatus.REJECTED
            );

            payment.setVerifiedAt(
                    LocalDateTime.now()
            );

            payment.setRejectionReason(
                    request.rejectionReason()
            );

            subscription.setStatus(
                    SubscriptionStatus.PENDING
            );

            Long paymentSubmitterId = payment.getCreatedBy();

            appUserRepository.findById(paymentSubmitterId)
                    .ifPresentOrElse(
                            appUser -> emailService.sendSubscriptionPaymentRejectedEmail(
                                    appUser.getEmail(),
                                    appUser.getFirstName(),
                                    subscription.getClinic().getName(),
                                    subscription.getPlan().getName(),
                                    payment.getTransactionId(),
                                    payment.getAmount(),
                                    request.rejectionReason()
                            ),
                            () -> log.warn("AppUser not found for id: {}", paymentSubmitterId)
                    );
        }

        return toResponse(payment);
    }

    /**
     * SUPER_ADMIN:
     * Get all pending payments.
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPendingPayments() {

        return paymentRepository
                .findByStatusOrderByCreatedAtDesc(
                        SubscriptionPaymentStatus.PENDING
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Clinic:
     * Get payment history.
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getClinicPayments(
            Long clinicId
    ) {

        return paymentRepository
                .findBySubscriptionClinicIdOrderByCreatedAtDesc(
                        clinicId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(
            Long paymentId
    ) {

        SubscriptionPayment payment =
                paymentRepository.findById(paymentId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Payment not found: "
                                                + paymentId
                                ));

        return toResponse(payment);
    }

    private PaymentResponse toResponse(
            SubscriptionPayment payment
    ) {

        ClinicSubscription subscription =
                payment.getSubscription();

        SubscriptionPlan plan =
                subscription.getPlan();

        return new PaymentResponse(
                payment.getId(),
                subscription.getClinic().getId(),
                subscription.getClinic().getName(),
                subscription.getId(),
                plan.getId(),
                plan.getName(),
                payment.getTransactionId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getPaymentDate(),
                payment.getVerifiedAt(),
                payment.getRejectionReason()
        );
    }
}