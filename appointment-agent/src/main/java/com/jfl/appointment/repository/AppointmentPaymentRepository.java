package com.jfl.appointment.repository;

import com.jfl.appointment.entity.AppointmentPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppointmentPaymentRepository
        extends JpaRepository<AppointmentPayment, Long> {

    Optional<AppointmentPayment> findByAppointmentId(Long appointmentId);

    boolean existsByAppointmentId(Long appointmentId);
}