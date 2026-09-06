package com.jfl.appointment.repository;

import com.jfl.appointment.entity.Notification;
import com.jfl.appointment.entity.NotificationChannel;
import com.jfl.appointment.entity.NotificationStatus;
import com.jfl.appointment.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    boolean existsByAppointmentIdAndTypeAndChannel(
            Long appointmentId,
            NotificationType type,
            NotificationChannel channel
    );

    List<Notification> findByStatusAndScheduledAtLessThanEqual(
            NotificationStatus status,
            LocalDateTime scheduledAt
    );


    List<Notification> findByAppointmentIdOrderByCreatedAtDesc(
            Long appointmentId
    );

    // Used when rescheduling/cancelling an appointment - find the still-pending
    // notification for it so we can update or invalidate it instead of leaving
    // a stale scheduled_at pointing at the old time.
    Optional<Notification> findByAppointmentIdAndTypeAndChannelAndStatus(
            Long appointmentId, NotificationType type, NotificationChannel channel, NotificationStatus status);

    // The dispatch poll: due = PENDING and scheduled_at has arrived. JOIN FETCH
    // pulls appointment/doctor/service/patient in one query since the dispatch
    // DTO needs all of them (template variables).
    @Query("select n from Notification n " +
            "join fetch n.appointment a join fetch a.doctor join fetch a.service join fetch a.patient " +
            "where n.status = 'PENDING' and n.channel = :channel and n.scheduledAt <= :now " +
            "order by n.scheduledAt asc")
    List<Notification> findDue(@Param("channel") NotificationChannel channel, @Param("now") LocalDateTime now);


    /**
     * Step 1 of the claim pattern. A plain UPDATE ... WHERE status = 'PENDING'
     * is naturally race-safe under Postgres's default READ COMMITTED isolation:
     * if two overlapping dispatch runs execute this at the same time, the DB
     * row-locks whichever rows the first UPDATE touches. The second UPDATE
     * blocks on those specific rows until the first commits - at which point
     * they're no longer status='PENDING', so its own WHERE clause simply
     * doesn't match them anymore. No explicit application-level locking needed;
     * this is the standard job-queue "claim" pattern.
     */
    @Modifying
    @Query(value = "UPDATE notification SET status = 'DISPATCHING', updated_at = now() " +
            "WHERE status = 'PENDING' AND channel = :channel AND scheduled_at <= :now",
            nativeQuery = true)
    int claimDue(@Param("channel") String channel, @Param("now") LocalDateTime now);

    /**
     * Safety net for a dispatch run that crashes mid-batch (e.g. n8n execution
     * errors out after claiming but before every item gets mark-sent/mark-failed
     * called). Anything stuck in DISPATCHING for longer than the stale threshold
     * goes back to PENDING so the next run picks it up again, rather than being
     * silently lost forever.
     */
    @Modifying
    @Query(value = "UPDATE notification SET status = 'PENDING', updated_at = now() " +
            "WHERE status = 'DISPATCHING' AND updated_at < :staleThreshold",
            nativeQuery = true)
    int reclaimStale(@Param("staleThreshold") LocalDateTime staleThreshold);

    // Step 2 of the claim pattern: fetch the rows this run just claimed, with
    // everything the dispatch DTO needs in one query.
    @Query("select n from Notification n " +
            "join fetch n.appointment a join fetch a.doctor join fetch a.service join fetch a.patient " +
            "where n.status = 'DISPATCHING' and n.channel = :channel " +
            "order by n.scheduledAt asc")
    List<Notification> findClaimed(@Param("channel") NotificationChannel channel);
}
