package com.jfl.appointment.dashboard.service;


import com.jfl.appointment.dashboard.dto.NotificationDueDto;
import com.jfl.appointment.entity.*;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

    private static final int STALE_DISPATCH_MINUTES = 20;

    private final NotificationRepository notificationRepository;

    /**
     * Three steps, one transaction:
     *   1. Reclaim anything stuck in DISPATCHING too long (a previous run that
     *      crashed mid-batch) back to PENDING, so it isn't lost forever.
     *   2. Atomically claim everything currently due - PENDING -> DISPATCHING.
     *      This is the step that makes overlapping 15-min runs safe: whichever
     *      run's UPDATE commits first "wins" those rows; see
     *      NotificationRepository.claimDue for why this needs no extra locking.
     *   3. Fetch exactly what THIS run just claimed and hand it to n8n.
     */
    @Transactional
    public List<NotificationDueDto> findDue(NotificationChannel channel) {
        notificationRepository.reclaimStale(LocalDateTime.now().minusMinutes(STALE_DISPATCH_MINUTES));
        notificationRepository.claimDue(channel.name(), LocalDateTime.now());
        List<Notification> claimed = notificationRepository.findClaimed(channel);

        // Safety net: an appointment can be cancelled after its reminder was
        // already scheduled. Reschedule updates the existing row (see
        // NotificationSchedulingService), but cancellation doesn't touch
        // notification rows at all - so catch it here, right before handing
        // it to n8n to send.
        claimed.removeIf(n -> {
            boolean stillValid = n.getAppointment().getStatus() == AppointmentStatus.CONFIRMED;
            if (!stillValid) {
                n.setStatus(NotificationStatus.FAILED);
                n.setErrorMessage("Appointment no longer CONFIRMED (status: " + n.getAppointment().getStatus() + ") at dispatch time");
            }
            return !stillValid;
        });

        return claimed.stream()
                .map(n -> new NotificationDueDto(
                        n.getId(),
                        n.getAppointment().getId(),
                        n.getType(),
                        n.getAppointment().getPatient().getName(),
                        n.getAppointment().getPatient().getWhatsappNumber(),
                        n.getAppointment().getDoctor().getName(),
                        n.getAppointment().getService().getName(),
                        n.getAppointment().getAppointmentDate(),
                        n.getAppointment().getStartTime()))
                .toList();
    }

    @Transactional
    public void markSent(Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found: " + notificationId));
        n.setStatus(NotificationStatus.SENT);
        n.setSentAt(LocalDateTime.now());
    }

    @Transactional
    public void markFailed(Long notificationId, String errorMessage) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found: " + notificationId));
        n.setStatus(NotificationStatus.FAILED);
        n.setErrorMessage(errorMessage);
    }
}