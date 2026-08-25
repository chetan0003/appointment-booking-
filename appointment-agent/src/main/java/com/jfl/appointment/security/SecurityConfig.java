package com.jfl.appointment.security;


import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider();

        provider.setUserDetailsService(
                userDetailsService
        );

        provider.setPasswordEncoder(
                passwordEncoder()
        );

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {

        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .cors(cors -> {})

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Authentication
                        .requestMatchers(
                                "/api/auth/**"
                        ).permitAll()

                        // Public WhatsApp/n8n APIs
                        .requestMatchers(
                                "/api/clinics/*/services",
                                "/api/clinics/*/doctors",
                                "/api/clinics/*/availability",
                                "/api/sessions/**",
                                "/api/appointments",
                                "/api/n8n/**"
                        ).permitAll()

                        // Super admin
                        .requestMatchers(
                                "/api/admin/**"
                        ).hasRole("SUPER_ADMIN")

                        // Clinic administration
                        .requestMatchers(
                                "/api/clinic-admin/**"
                        ).hasAnyRole(
                                "SUPER_ADMIN",
                                "CLINIC_ADMIN"
                        )

                        // Staff
                        .requestMatchers(
                                "/api/staff/**"
                        ).hasAnyRole(
                                "SUPER_ADMIN",
                                "CLINIC_ADMIN",
                                "STAFF"
                        )

                        // Doctor
                        .requestMatchers(
                                "/api/doctor/**"
                        ).hasAnyRole(
                                "SUPER_ADMIN",
                                "CLINIC_ADMIN",
                                "DOCTOR"
                        )

                        .anyRequest()
                        .authenticated()
                )

                .authenticationProvider(
                        authenticationProvider()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
