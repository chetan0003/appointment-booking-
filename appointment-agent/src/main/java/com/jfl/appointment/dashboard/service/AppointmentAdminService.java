package com.jfl.appointment.dashboard.service;

import com.jfl.appointment.dashboard.dto.AppointmentListItemDto;
import com.jfl.appointment.entity.Appointment;
import com.jfl.appointment.entity.AppointmentStatus;
import com.jfl.appointment.entity.ClinicUser;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.exception.SlotUnavailableException;
import com.jfl.appointment.n8n.service.AvailabilityService;
import com.jfl.appointment.repository.AppointmentRepository;
import com.jfl.appointment.security.SecurityContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentAdminService {

    private final AppointmentRepository appointmentRepository;
    private final AvailabilityService availabilityService;
    private final ClinicAccessService clinicAccessService;
    private final SecurityContextService securityContextService;

    @Transactional(readOnly = true)
    public List<AppointmentListItemDto> listAppointments(Long clinicId, LocalDate from, LocalDate to,
                                                         Long doctorId, AppointmentStatus status,Long serviceId) {

        if (!clinicAccessService
                .hasAccessToClinic(clinicId)) {

            throw new SecurityException(
                    "No access to clinic"
            );
        }

        if (securityContextService.hasRole("DOCTOR")) {

            ClinicUser clinicUser =
                    clinicAccessService
                            .getClinicUser(clinicId);

            doctorId =
                    clinicUser.getDoctor().getId();
        }

        return appointmentRepository
                .findForDashboard(
                        clinicId,
                        serviceId,
                        doctorId,
                        status
                )
                .stream()
                .map(this::toDto)
                .toList();
//        return appointmentRepository.findForDashboard(clinicId).stream()
//                .filter(a -> doctorId == null || a.getDoctor().getId().equals(doctorId))
//                .filter(a -> status == null || a.getStatus() == status)
//                .map(this::toDto)
//                .toList();
    }


    /**
     * Cancelling just flips the status. Nothing else to do at the DB level -
     * uq_doctor_slot_active is a PARTIAL unique index (WHERE status = 'CONFIRMED'),
     * so the moment this row stops being CONFIRMED its slot is automatically free
     * for a new booking. No separate "release the slot" step needed.
     */
    @Transactional
    public AppointmentListItemDto cancelAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found: " + appointmentId));

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new SlotUnavailableException(
                    "Only a CONFIRMED appointment can be cancelled (current status: " + appointment.getStatus() + ")");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        return toDto(appointment);
    }

    /**
     * Same "lock, re-check, then act" pattern as DashboardAppointmentService.createAppointment -
     * see that class for the full rationale. Here the extra piece is excluding this
     * appointment's own id from the availability check, since it still holds its old
     * slot right up until this transaction commits.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AppointmentListItemDto rescheduleAppointment(Long appointmentId, LocalDate newDate, LocalTime newStartTime) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found: " + appointmentId));

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new SlotUnavailableException(
                    "Only a CONFIRMED appointment can be rescheduled (current status: " + appointment.getStatus() + ")");
        }

        appointmentRepository.lockDoctorAppointmentsForDate(appointment.getDoctor().getId(), newDate);

        List<LocalTime> freshSlots = availabilityService.computeSlots(
                appointment.getClinic().getId(), appointment.getDoctor(), appointment.getService(),
                newDate, appointment.getId());

        if (!freshSlots.contains(newStartTime)) {
            throw new SlotUnavailableException(
                    "Requested slot " + newStartTime + " on " + newDate + " is not available for this doctor.");
        }

        appointment.setAppointmentDate(newDate);
        appointment.setStartTime(newStartTime);
        appointment.setEndTime(newStartTime.plusMinutes(appointment.getService().getDurationMinutes()));

        return toDto(appointment);
    }

    private AppointmentListItemDto toDto(Appointment a) {
        return new AppointmentListItemDto(
                a.getId(),
                a.getAppointmentCode(),
                a.getAppointmentDate(),
                a.getStartTime(),
                a.getEndTime(),
                a.getStatus(),
                a.getDoctor().getId(),
                a.getDoctor().getName(),
                a.getService().getId(),
                a.getService().getName(),
                a.getPatient().getName(),
                a.getPatient().getWhatsappNumber()
        );
    }

    public void validateStatusTransition(
            AppointmentStatus current,
            AppointmentStatus next) {

        boolean valid = switch (current) {

            case CONFIRMED ->
                    next == AppointmentStatus.CHECKED_IN
                            || next == AppointmentStatus.CANCELLED
                            || next == AppointmentStatus.NO_SHOW;

            case CHECKED_IN ->
                    next == AppointmentStatus.WAITING
                            || next == AppointmentStatus.CANCELLED;

            case WAITING ->
                    next == AppointmentStatus.IN_PROGRESS;

            case IN_PROGRESS ->
                    next == AppointmentStatus.COMPLETED;

            case COMPLETED, CANCELLED, NO_SHOW ->
                    false;
        };

        if (!valid) {
            throw new IllegalStateException(
                    "Invalid appointment status transition: "
                            + current + " -> " + next
            );
        }
    }
}
