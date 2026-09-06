package com.jfl.appointment.dashboard.service;


import com.jfl.appointment.entity.*;
import com.jfl.appointment.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Decides WHEN a notification should fire and writes that decision to the
 * notification table. Called synchronously from AppointmentService /
 * AppointmentAdminService, inside the SAME transaction as the appointment
 * write that triggered it - so it's never possible to have a CONFIRMED
 * appointment with no reminder scheduled, or a rescheduled appointment whose
 * reminder still points at the old time. Contains no I/O of its own (no
 * WhatsApp calls) - that's Phase 2's job (NotificationDispatchService).
 */
@Service
@RequiredArgsConstructor
public class NotificationSchedulingService {

    private final NotificationRepository notificationRepository;

    public void scheduleBookingReminder(Appointment appointment) {
        Notification n = new Notification();
        n.setAppointment(appointment);
        n.setType(NotificationType.REMINDER_24H);
        n.setChannel(NotificationChannel.WHATSAPP);
        n.setStatus(NotificationStatus.PENDING);
        n.setScheduledAt(appointmentDateTime(appointment).minusHours(24));
        notificationRepository.save(n);
    }

    /**
     * Called after a successful reschedule. Moves the existing PENDING reminder
     * to the new time instead of leaving it pointing at the old slot. If none
     * exists yet (e.g. reminder already fired before the reschedule happened),
     * creates a fresh one for the new time.
     */
    public void rescheduleBookingReminder(Appointment appointment) {
        notificationRepository.findByAppointmentIdAndTypeAndChannelAndStatus(
                        appointment.getId(), NotificationType.REMINDER_24H,
                        NotificationChannel.WHATSAPP, NotificationStatus.PENDING)
                .ifPresentOrElse(
                        existing -> existing.setScheduledAt(appointmentDateTime(appointment).minusHours(24)),
                        () -> scheduleBookingReminder(appointment)
                );
    }

    /**
     * Called when staff marks a visit COMPLETED with a suggested follow-up date.
     * Fires at 10am on that date - a fixed, sane default rather than a precise
     * time nobody set (there's no "slot" for a nudge message the way there is
     * for an actual appointment).
     */
    public void scheduleFollowUpSuggestion(Appointment completedAppointment, LocalDate suggestedDate) {
        Notification n = new Notification();
        n.setAppointment(completedAppointment);
        n.setType(NotificationType.FOLLOW_UP_SUGGESTED);
        n.setChannel(NotificationChannel.WHATSAPP);
        n.setStatus(NotificationStatus.PENDING);
        n.setScheduledAt(LocalDateTime.of(suggestedDate, LocalTime.of(10, 0)));
        notificationRepository.save(n);
    }

    /**
     * Called after STAFF reschedules from the dashboard (as opposed to a patient
     * rescheduling mid-conversation, which would just reply directly - no async
     * notice needed there, see design discussion). Fires immediately since the
     * patient needs to know now, not "sometime before their old slot".
     */
    public void scheduleRescheduledNotice(Appointment appointment) {
        Notification n = new Notification();
        n.setAppointment(appointment);
        n.setType(NotificationType.RESCHEDULED);
        n.setChannel(NotificationChannel.WHATSAPP);
        n.setStatus(NotificationStatus.PENDING);
        n.setScheduledAt(LocalDateTime.now());
        notificationRepository.save(n);
    }

    private LocalDateTime appointmentDateTime(Appointment a) {
        return LocalDateTime.of(a.getAppointmentDate(), a.getStartTime());
    }
}
