package com.jfl.appointment.repository;


import com.jfl.appointment.entity.LoginChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface LoginChallengeRepository
        extends JpaRepository<LoginChallenge, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c FROM LoginChallenge c
        WHERE c.tokenHash = :tokenHash
        """)
    Optional<LoginChallenge> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );
}
