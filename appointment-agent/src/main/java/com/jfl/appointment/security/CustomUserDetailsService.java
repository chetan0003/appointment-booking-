package com.jfl.appointment.security;


import com.jfl.appointment.entity.AppUser;
import com.jfl.appointment.entity.ClinicUser;
import com.jfl.appointment.repository.AppUserRepository;
import com.jfl.appointment.repository.ClinicUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

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
        ClinicUser clinicUser = clinicUserRepository.findByUser_Id(user.getId()).orElseThrow();
        Long clinicId = clinicUser.getClinic().getId();
        return new CustomUserDetails(user,clinicId);
    }
}
