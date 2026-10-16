package com.jfl.appointment.dashboard.service;


import com.jfl.appointment.entity.AppUser;
import com.jfl.appointment.entity.LoginChallenge;
import com.jfl.appointment.repository.AppUserRepository;
import com.jfl.appointment.repository.LoginChallengeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

import static org.apache.commons.codec.digest.DigestUtils.sha256;

@Service
@RequiredArgsConstructor
public class LoginChallengeService {


    private final LoginChallengeRepository loginChallengeRepository;
    private final AppUserRepository appUserRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String createChallenge(Long userId) {

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        LoginChallenge challenge = new LoginChallenge();
        challenge.setUserId(userId);
        challenge.setTokenHash(sha256(rawToken));
        challenge.setExpiresAt(
                Instant.now().plus(5, ChronoUnit.MINUTES)
        );

        loginChallengeRepository.save(challenge);

        // Return the raw token only once.
        // Store only its hash in the database.
        return rawToken;
    }

    @Transactional(readOnly = true)
    public Long validateChallenge(String rawToken) throws AccessDeniedException {

        if (rawToken == null || rawToken.isBlank()) {
            throw new AccessDeniedException(
                    "Invalid or expired login challenge"
            );
        }

        LoginChallenge challenge = loginChallengeRepository
                .findByTokenHashForUpdate(sha256(rawToken))
                .orElseThrow(() -> new AccessDeniedException(
                        "Invalid or expired login challenge"
                ));

        if (challenge.getUsedAt() != null ||
                !challenge.getExpiresAt().isAfter(Instant.now())) {
            throw new AccessDeniedException(
                    "Invalid or expired login challenge"
            );
        }

        return challenge.getUserId();
    }

    @Transactional
    public void consumeChallenge(String rawToken) throws AccessDeniedException {

        LoginChallenge challenge = loginChallengeRepository
                .findByTokenHashForUpdate(sha256(rawToken))
                .orElseThrow(() -> new AccessDeniedException(
                        "Invalid or expired login challenge"
                ));

        if (challenge.getUsedAt() != null ||
                !challenge.getExpiresAt().isAfter(Instant.now())) {
            throw new AccessDeniedException(
                    "Invalid or expired login challenge"
            );
        }

        challenge.setUsedAt(Instant.now());
        loginChallengeRepository.save(challenge);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(
                    digest.digest(
                            value.getBytes(StandardCharsets.UTF_8)
                    )
            );
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Unable to hash login challenge", ex
            );
        }
    }

    @Transactional
    public Long verifyAndConsume(
            String rawToken,
            String code,
            TotpService totpService) {

        if (rawToken == null || rawToken.isBlank()
                || code == null
                || !code.matches("\\d{6}")) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Invalid or expired login challenge"
            );
        }

        String tokenHash = sha256(rawToken);

        LoginChallenge challenge = loginChallengeRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() ->
                        new org.springframework.security.access.AccessDeniedException(
                                "Invalid or expired login challenge"
                        ));

        Instant now = Instant.now();

        if (challenge.getUsedAt() != null
                || !challenge.getExpiresAt().isAfter(now)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Invalid or expired login challenge"
            );
        }

        AppUser user = appUserRepository.findById(challenge.getUserId())
                .orElseThrow(() ->
                        new org.springframework.security.access.AccessDeniedException(
                                "Invalid or expired login challenge"
                        ));

        if (!user.isTotpEnabled()
                || user.getTotpSecret() == null
                || !totpService.verifyCode(
                user.getTotpSecret(),
                Integer.parseInt(code))) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Invalid authenticator code"
            );
        }

        // Consume the challenge only after successful TOTP verification.
        challenge.setUsedAt(now);

        loginChallengeRepository.save(challenge);

        return user.getId();
    }
}
