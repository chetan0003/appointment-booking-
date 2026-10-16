package com.jfl.appointment.n8n.service;

import com.jfl.appointment.dashboard.dto.AppointmentListItemDto;
import com.jfl.appointment.dashboard.service.AppointmentAdminService;
import com.jfl.appointment.dashboard.service.NotificationSchedulingService;
import com.jfl.appointment.dashboard.service.NotificationService;
import com.jfl.appointment.entity.*;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.exception.SlotUnavailableException;
import com.jfl.appointment.n8n.dto.AppointmentResponse;
import com.jfl.appointment.n8n.dto.CreateAppointmentRequest;
import com.jfl.appointment.repository.*;
import com.jfl.appointment.security.IntegrationUtil;
import com.jfl.appointment.service.SubscriptionFeatureService;
import com.jfl.appointment.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppointmentService {


    private final AppointmentAdminService appointmentAdminService;

    /**
     * Runs in its own REQUIRES_NEW transaction so the pessimistic lock is
     * held for the shortest possible window and released as soon as this
     * method returns (commit/rollback), rather than for the lifetime of
     * whatever larger transaction (e.g. a conversation-session update)
     * might be calling it.
     *
     * Flow (design doc section 15/16, "check twice" + "prevent double booking"):
     *   1. Pessimistic-lock this doctor's CONFIRMED appointments for the date
     *      -> any concurrent booking attempt for the same doctor/date blocks here.
     *   2. Re-run slot computation (the SECOND, authoritative check).
     *   3. If the requested slot isn't in the fresh list -> reject.
     *   4. Insert. The partial unique index in schema.sql is the final safety
     *      net if this logic is ever bypassed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AppointmentResponse createAppointment(CreateAppointmentRequest request) {
        log.info("createAppointment: {},{},{}",request.clinicId(), request.patientName(),request.appointmentDate());
        log.info("Session Code :: {} , Session Id :: {}", request.sessionCode(), request.sessionId());
        com.jfl.appointment.dashboard.dto.CreateAppointmentRequest createAppointmentRequest = convertDashboardAppointmentReq(request);
        AppointmentListItemDto appointment = appointmentAdminService.createAppointment(request.clinicId(), createAppointmentRequest);
        return toResponse(appointment);
    }

    public com.jfl.appointment.dashboard.dto.CreateAppointmentRequest convertDashboardAppointmentReq(com.jfl.appointment.n8n.dto.CreateAppointmentRequest newSource) {
        return new com.jfl.appointment.dashboard.dto.CreateAppointmentRequest(
                newSource.clinicId(),
                newSource.patientId(),
                newSource.doctorId(),
                newSource.serviceId(),
                newSource.patientName(),
                newSource.qrType(),
                newSource.whatsappNumber(),
                newSource.appointmentDate(),
                newSource.startTime(),
                null,
                newSource.idempotencyKey(),
                newSource.sessionId(),
                newSource.sessionCode(),
                null,
                PatientSource.WHATSAPP.name()
        );
    }


    private AppointmentResponse toResponse(AppointmentListItemDto a) {
        return new AppointmentResponse(
                a.appointmentCode(),
                a.status(),
                a.appointmentDate(),
                a.startTime(),
                a.endTime(),
                a.doctorName(),
                a.serviceName(),
                a.rawToken()
        );
    }
}
