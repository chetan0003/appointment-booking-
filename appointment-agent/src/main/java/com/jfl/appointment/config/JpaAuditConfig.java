package com.jfl.appointment.config;

import com.jfl.appointment.security.SecurityContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Slf4j
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@RequiredArgsConstructor
public class JpaAuditConfig {

    private final SecurityContextService securityContextService;

    @Bean
    public AuditorAware<Long> auditorProvider() {

        return () -> {
            try {
                Optional<Long> currentUserId = Optional.of(
                        securityContextService.getCurrentUserId()
                );
                log.info("AUDIT USER ID = {}",currentUserId.get());
                return currentUserId;
            } catch (IllegalStateException e) {
                // No authenticated user
                // Example: n8n / WhatsApp / system operation
                return Optional.empty();
            }
        };
    }
}