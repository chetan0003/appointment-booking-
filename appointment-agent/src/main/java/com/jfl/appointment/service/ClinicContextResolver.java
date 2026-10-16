package com.jfl.appointment.service;

import com.jfl.appointment.repository.ClinicRepository;
import com.jfl.appointment.security.SecurityContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ClinicContextResolver {

    private final SecurityContextService securityContextService;
    private final ClinicRepository clinicRepository;

    public Long resolveClinicId(Long requestedClinicId) {

        // SUPER_ADMIN can select any existing clinic
        if (securityContextService.hasRole("SUPER_ADMIN")) {

            if (requestedClinicId == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Please select a clinic"
                );
            }

            if (!clinicRepository.existsById(requestedClinicId)) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Clinic not found"
                );
            }

            return requestedClinicId;
        }

        // Other users are restricted to their assigned clinic
        Long assignedClinicId =
                securityContextService.getClinicId();

        if (assignedClinicId == null) {
            throw new AccessDeniedException(
                    "No clinic assigned to current user"
            );
        }

        // Prevent cross-clinic access
        if (requestedClinicId != null
                && !requestedClinicId.equals(assignedClinicId)) {

            throw new AccessDeniedException(
                    "You cannot access another clinic"
            );
        }

        return assignedClinicId;
    }
}
