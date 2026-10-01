package com.jfl.appointment.service;

import com.jfl.appointment.dto.AppointmentPaymentResponse;
import com.jfl.appointment.dto.CollectPaymentRequest;
import com.jfl.appointment.entity.Appointment;
import com.jfl.appointment.entity.AppointmentPayment;
import com.jfl.appointment.entity.AppointmentPaymentStatus;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.repository.AppointmentPaymentRepository;
import com.jfl.appointment.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AppointmentPaymentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentPaymentRepository paymentRepository;

    @Transactional
    public AppointmentPaymentResponse collectPayment(
            Long appointmentId,
            CollectPaymentRequest request
    ) {

        AppointmentPayment payment =
                paymentRepository.findByAppointmentId(appointmentId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Payment record not found for appointment: "
                                                + appointmentId
                                ));

        Appointment appointment =
                appointmentRepository.findById(appointmentId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Appoitment record not found for payment: "
                                                + payment.getId()
                                ));

        BigDecimal paymentAmount = request.amount();

        BigDecimal remainingAmount =
                payment.getTotalAmount()
                        .subtract(payment.getPaidAmount());

        if (paymentAmount.compareTo(remainingAmount) > 0) {
            throw new IllegalArgumentException(
                    "Payment amount cannot be greater than remaining amount"
            );
        }

        BigDecimal newPaidAmount =
                payment.getPaidAmount().add(paymentAmount);

        payment.setPaidAmount(newPaidAmount);
        payment.setPaymentMethod(request.paymentMethod());

        if (newPaidAmount.compareTo(payment.getTotalAmount()) == 0) {

            payment.setStatus(AppointmentPaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            appointment.setPaymentStatus(AppointmentPaymentStatus.PAID);

        } else {
            payment.setStatus(AppointmentPaymentStatus.PARTIAL);
            appointment.setPaymentStatus(AppointmentPaymentStatus.PARTIAL);
        }

        AppointmentPayment saved =
                paymentRepository.save(payment);
        appointmentRepository.save(appointment);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AppointmentPaymentResponse getPayment(Long appointmentId) {

        AppointmentPayment payment =
                paymentRepository.findByAppointmentId(appointmentId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Payment record not found for appointment: "
                                                + appointmentId
                                ));

        return toResponse(payment);
    }

    private AppointmentPaymentResponse toResponse(
            AppointmentPayment payment
    ) {

        BigDecimal remaining =
                payment.getTotalAmount()
                        .subtract(payment.getPaidAmount());

        return new AppointmentPaymentResponse(
                payment.getId(),
                payment.getAppointment().getId(),
                payment.getTotalAmount(),
                payment.getPaidAmount(),
                remaining,
                payment.getStatus(),
                payment.getPaymentMethod(),
                payment.getPaidAt()
        );
    }
}