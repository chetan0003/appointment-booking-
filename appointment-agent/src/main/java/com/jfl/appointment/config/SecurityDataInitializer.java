package com.jfl.appointment.config;

import com.jfl.appointment.entity.AppUser;
import com.jfl.appointment.entity.Role;
import com.jfl.appointment.entity.RoleName;
import com.jfl.appointment.repository.AppUserRepository;
import com.jfl.appointment.repository.RoleRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class SecurityDataInitializer {

    private final RoleRepository roleRepository;

    private final AppUserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initSecurityData() {

        return args -> {

            for (RoleName roleName :
                    RoleName.values()) {

                roleRepository
                        .findByName(roleName)
                        .orElseGet(() ->
                                roleRepository.save(
                                        Role.builder()
                                                .name(roleName)
                                                .build()
                                )
                        );
            }

            if (!userRepository
                    .existsByUsername("superadmin")) {

                Role role =
                        roleRepository
                                .findByName(
                                        RoleName.SUPER_ADMIN
                                )
                                .orElseThrow();

                AppUser user =
                        AppUser.builder()
                                .username("superadmin")
                                .email(
                                        "superadmin@example.com"
                                )
                                .password(
                                        passwordEncoder.encode(
                                                "ChangeMe@123"
                                        )
                                )
                                .firstName("System")
                                .lastName("Administrator")
                                .enabled(true)
                                .build();

                user.getRoles().add(role);

                userRepository.save(user);
            }
        };
    }
}
