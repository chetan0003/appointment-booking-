package com.jfl.appointment.dashboard.service;

import com.jfl.appointment.entity.AppUser;
import com.jfl.appointment.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final Duration LOCK_DURATION =
            Duration.ofMinutes(15);

    private final AppUserRepository userRepository;

    @Transactional(readOnly = true)
    public void checkLocked(AppUser user) {
        Instant lockedUntil = user.getLockedUntil();

        if (lockedUntil != null &&
                lockedUntil.isAfter(Instant.now())) {
            throw new AccessDeniedException(
                    "Account temporarily locked"
            );
        }
    }

    @Transactional
    public void recordFailure(Long userId) {
        AppUser user = userRepository.findByIdForUpdate(userId)
                .orElseThrow();

        Instant now = Instant.now();
        Instant lockedUntil = user.getLockedUntil();

        if (lockedUntil != null && lockedUntil.isAfter(now)) {
            return;
        }

        user.setFailedLoginAttempts(
                user.getFailedLoginAttempts() + 1
        );

        if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(now.plus(LOCK_DURATION));
        }

        userRepository.save(user);
    }

    @Transactional
    public void recordSuccess(Long userId) {
        AppUser user = userRepository.findByIdForUpdate(userId)
                .orElseThrow();

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        userRepository.save(user);
    }
}
