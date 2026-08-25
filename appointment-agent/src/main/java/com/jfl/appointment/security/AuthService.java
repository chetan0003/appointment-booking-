package com.jfl.appointment.security;



import com.jfl.appointment.dashboard.dto.LoginRequest;
import com.jfl.appointment.dashboard.dto.LoginResponse;
import com.jfl.appointment.entity.AppUser;
import com.jfl.appointment.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;

    private final AppUserRepository userRepository;

    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        AppUser user =
                userRepository
                        .findByUsername(request.username())
                        .orElseThrow();

        CustomUserDetails userDetails =
                new CustomUserDetails(user);

        String token =
                jwtService.generateToken(userDetails);

        return new LoginResponse(

                token,

                user.getId(),

                user.getUsername(),

                user.getRoles()
                        .stream()
                        .map(role ->
                                role.getName().name()
                        )
                        .toList()
        );
    }
}
