package com.jfl.appointment.security;


import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityContextService {

    public CustomUserDetails getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                !(authentication.getPrincipal()
                        instanceof CustomUserDetails)) {

            throw new IllegalStateException(
                    "No authenticated user"
            );
        }

        return (CustomUserDetails)
                authentication.getPrincipal();
    }

    public Long getCurrentUserId() {

        return getCurrentUser().getUserId();
    }

    public String getCurrentUsername() {

        return getCurrentUser().getUsername();
    }

    public boolean hasRole(String role) {

        return getCurrentUser()
                .getAuthorities()
                .stream()
                .anyMatch(a ->
                        a.getAuthority()
                                .equals("ROLE_" + role)
                );
    }
}
