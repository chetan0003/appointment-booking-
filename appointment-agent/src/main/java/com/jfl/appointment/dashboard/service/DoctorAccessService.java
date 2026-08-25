package com.jfl.appointment.dashboard.service;


import com.jfl.appointment.dashboard.service.ClinicAccessService;
import com.jfl.appointment.entity.ClinicUser;
import com.jfl.appointment.security.SecurityContextService;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DoctorAccessService {

    private final ClinicAccessService clinicAccessService;

    private final SecurityContextService securityContextService;

    public void verifyDoctorAccess(
            Long clinicId,
            Long doctorId) {

        if (securityContextService.hasRole(
                "SUPER_ADMIN")) {

            return;
        }

        ClinicUser clinicUser =
                clinicAccessService.getClinicUser(
                        clinicId
                );

        if (securityContextService.hasRole(
                "DOCTOR")) {

            if (clinicUser.getDoctor() == null ||
                    !clinicUser.getDoctor()
                            .getId()
                            .equals(doctorId)) {

                throw new SecurityException(
                        "Doctor cannot access another doctor's data"
                );
            }
        }
    }
}
