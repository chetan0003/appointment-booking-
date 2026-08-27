package com.jfl.appointment.dashboard.service;


import com.jfl.appointment.dashboard.dto.ClinicUserDto;
import com.jfl.appointment.dashboard.dto.CreateUserRequest;
import com.jfl.appointment.dashboard.dto.UserResponse;
import com.jfl.appointment.entity.*;
import com.jfl.appointment.repository.*;
import com.jfl.appointment.security.SecurityContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final AppUserRepository userRepository;

    private final RoleRepository roleRepository;

    private final ClinicUserRepository clinicUserRepository;

    private final ClinicRepository clinicRepository;

    private final DoctorRepository doctorRepository;

    private final PasswordEncoder passwordEncoder;

    private final SecurityContextService securityContextService;
    private final AppUserRepository appUserRepository;

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

    public List<ClinicUserDto> getClinicUsersByClientId(Long clinicId) {
        return clinicUserRepository.findByClientId(clinicId).stream().map(this::toDto)
                .toList();
    }

    public UserResponse getUserDetail() {
        Long currentUserId = securityContextService.getCurrentUserId();
        return appUserRepository.findById(currentUserId).map(this::toDto).get();
    }

    private ClinicUserDto toDto(ClinicUser s) {
        return new ClinicUserDto(s.getId(), s.getUser().getFirstName(), s.getUser().getEmail(), s.getUser().getRoles().stream().findFirst().get().getName().name(), s.getUser().isEnabled(), "Today");
    }

    private UserResponse toDto(AppUser appUser) {
        return new UserResponse(appUser.getId(),appUser.getUsername(),appUser.getEmail(),
                appUser.getFirstName(), appUser.getLastName(),
                appUser.getRoles().stream().findFirst().get().getName().name(),appUser.getPhone(),appUser.isEnabled());
    }
}