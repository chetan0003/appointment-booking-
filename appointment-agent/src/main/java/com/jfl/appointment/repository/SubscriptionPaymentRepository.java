package com.jfl.appointment.repository;


import com.jfl.appointment.entity.PaymentStatus;
import com.jfl.appointment.entity.SubscriptionPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionPaymentRepository
        extends JpaRepository<SubscriptionPayment, Long> {

    boolean existsByTransactionId(String transactionId);

    List<SubscriptionPayment> findByStatusOrderByCreatedAtDesc(
            PaymentStatus status
    );

    List<SubscriptionPayment>
    findBySubscriptionClinicIdOrderByCreatedAtDesc(
            Long clinicId
    );

    boolean existsBySubscriptionIdAndStatus(
            Long subscriptionId,
            PaymentStatus status
    );
}
