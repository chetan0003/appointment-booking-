package com.jfl.appointment.repository;

import com.jfl.appointment.entity.ConversationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConversationSessionRepository extends JpaRepository<ConversationSession, Long> {

    @Query("select s from ConversationSession s where s.clinic.id = :clinicId " +
           "and s.whatsappNumber = :whatsappNumber " +
           "and s.state not in (com.jfl.appointment.entity.ConversationState.BOOKED, " +
           "                    com.jfl.appointment.entity.ConversationState.ABANDONED)")
    Optional<ConversationSession> findActiveSession(
            @Param("clinicId") Long clinicId, @Param("whatsappNumber") String whatsappNumber);
}
