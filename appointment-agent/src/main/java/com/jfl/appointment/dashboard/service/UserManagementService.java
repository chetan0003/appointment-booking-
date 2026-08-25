package com.jfl.appointment.dashboard.service;


import com.jfl.appointment.dashboard.dto.CreateUserRequest;
import com.jfl.appointment.entity.*;
import com.jfl.appointment.repository.*;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final AppUserRepository userRepository;

    private final RoleRepository roleRepository;

    private final ClinicUserRepository clinicUserRepository;

    private final ClinicRepository clinicRepository;

    private final DoctorRepository doctorRepository;

    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AppUser createUser(
            CreateUserRequest request) {

        if (userRepository.existsByUsername(
                request.username())) {

            throw new IllegalArgumentException(
                    "Username already exists"
            );
        }

        AppUser user = AppUser.builder()
                .username(request.username())
                .email(request.email())
                .password(
                        passwordEncoder.encode(
                                request.password()
                        )
                )
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .enabled(true)
                .build();

        Role role =
                roleRepository
                        .findByName(request.role())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Role not found"
                                )
                        );

        user.getRoles().add(role);

        user = userRepository.save(user);

        /*
         * SUPER_ADMIN does not belong to a clinic.
         */
        if (request.role() == RoleName.SUPER_ADMIN) {

            return user;
        }

        if (request.clinicId() == null) {

            throw new IllegalArgumentException(
                    "clinicId is required"
            );
        }

        Clinic clinic =
                clinicRepository
                        .findById(request.clinicId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Clinic not found"
                                )
                        );

        Doctor doctor = null;

        if (request.role() == RoleName.DOCTOR) {

            if (request.doctorId() == null) {

                throw new IllegalArgumentException(
                        "doctorId is required for DOCTOR"
                );
            }

            doctor =
                    doctorRepository
                            .findById(
                                    request.doctorId()
                            )
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Doctor not found"
                                    )
                            );
        }

        ClinicUser clinicUser =
                ClinicUser.builder()
                        .user(user)
                        .clinic(clinic)
                        .doctor(doctor)
                        .active(true)
                        .build();

        clinicUserRepository.save(clinicUser);

        return user;
    }
}