package com.jfl.appointment.security;



import com.jfl.appointment.dashboard.dto.LoginRequest;
import com.jfl.appointment.dashboard.dto.LoginResponse;
import com.jfl.appointment.entity.AppUser;
import com.jfl.appointment.entity.ClinicUser;
import com.jfl.appointment.entity.RoleName;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.repository.AppUserRepository;
import com.jfl.appointment.repository.ClinicUserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository userRepository;
    private final JwtService jwtService;
    private final ClinicUserRepository clinicUserRepository;


    public LoginResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        AppUser user = userRepository
                .findByUsername(request.username())
                .orElseThrow(() ->
                        new NotFoundException("User not found"));

        boolean isSuperAdmin = user.getRoles().stream()
                .anyMatch(role ->
                        role.getName() == RoleName.SUPER_ADMIN
                );

        Long clinicId = clinicUserRepository
                .findByUser_Id(user.getId())
                .map(clinicUser -> clinicUser.getClinic().getId())
                .orElse(null);

        CustomUserDetails userDetails =
                new CustomUserDetails(user,clinicId);

        String token =
                jwtService.generateToken(userDetails);

        return new LoginResponse(

                token,

                user.getId(),

                user.getUsername(),
                userDetails.getClinicId(),
                user.getRoles()
                        .stream()
                        .map(role ->
                                role.getName().name()
                        )
                        .toList()
        );
    }
}
