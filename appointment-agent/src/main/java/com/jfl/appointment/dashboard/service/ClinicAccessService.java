package com.jfl.appointment.dashboard.service;


import com.jfl.appointment.entity.ClinicUser;
import com.jfl.appointment.repository.ClinicUserRepository;
import com.jfl.appointment.security.SecurityContextService;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClinicAccessService {

    private final ClinicUserRepository clinicUserRepository;

    private final SecurityContextService securityContext;

    public boolean hasAccessToClinic(Long clinicId) {

        if (securityContext.hasRole("SUPER_ADMIN")) {
            return true;
        }

        Long userId =
                securityContext.getCurrentUserId();

        return clinicUserRepository
                .findByUserIdAndClinicIdAndActiveTrue(
                        userId,
                        clinicId
                )
                .isPresent();
    }

    public ClinicUser getClinicUser(Long clinicId) {

        Long userId =
                securityContext.getCurrentUserId();

        return clinicUserRepository
                .findByUserIdAndClinicIdAndActiveTrue(
                        userId,
                        clinicId
                )
                .orElseThrow(() ->
                        new SecurityException(
                                "User does not have access to this clinic"
                        )
                );
    }
}
