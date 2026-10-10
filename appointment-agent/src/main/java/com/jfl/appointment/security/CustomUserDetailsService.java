package com.jfl.appointment.security;


import com.jfl.appointment.entity.AppUser;
import com.jfl.appointment.entity.ClinicUser;
import com.jfl.appointment.entity.RoleName;
import com.jfl.appointment.repository.AppUserRepository;
import com.jfl.appointment.repository.ClinicUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import javax.management.relation.RoleNotFoundException;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService
        implements UserDetailsService {

    private final AppUserRepository userRepository;
    private final ClinicUserRepository clinicUserRepository;


    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found: " + username
                        )
                );

        boolean isSuperAdmin = user.getRoles().stream()
                .anyMatch(role ->
                        role.getName() == RoleName.SUPER_ADMIN
                );

        Long clinicId = clinicUserRepository
                .findByUser_Id(user.getId())
                .map(clinicUser -> clinicUser.getClinic().getId())
                .orElse(null);

        if (!isSuperAdmin && clinicId == null) {
            throw new UsernameNotFoundException(
                    "User is not associated with a clinic: " + username
            );
        }

        return new CustomUserDetails(user, clinicId);
    }

}
