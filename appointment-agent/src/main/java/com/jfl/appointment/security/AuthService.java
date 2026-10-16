package com.jfl.appointment.security;


import com.jfl.appointment.dashboard.dto.*;
import com.jfl.appointment.dashboard.service.LoginAttemptService;
import com.jfl.appointment.dashboard.service.LoginChallengeService;
import com.jfl.appointment.dashboard.service.TotpService;
import com.jfl.appointment.entity.AppUser;
import com.jfl.appointment.entity.RoleName;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.repository.AppUserRepository;
import com.jfl.appointment.repository.ClinicUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository userRepository;
    private final JwtService jwtService;
    private final ClinicUserRepository clinicUserRepository;
    private final LoginAttemptService loginAttemptService;
    private final LoginChallengeService loginChallengeService;
    private final TotpService totpService;
    private final AppUserRepository appUserRepository;


//    public LoginResponse login(LoginRequest request) {
//
//        authenticationManager.authenticate(
//                new UsernamePasswordAuthenticationToken(
//                        request.username(),
//                        request.password()
//                )
//        );
//
//        AppUser user = userRepository
//                .findByUsername(request.username())
//                .orElseThrow(() ->
//                        new NotFoundException("User not found"));
//
//        boolean isSuperAdmin = user.getRoles().stream()
//                .anyMatch(role ->
//                        role.getName() == RoleName.SUPER_ADMIN
//                );
//
//        Long clinicId = clinicUserRepository
//                .findByUser_Id(user.getId())
//                .map(clinicUser -> clinicUser.getClinic().getId())
//                .orElse(null);
//
//        CustomUserDetails userDetails =
//                new CustomUserDetails(user,clinicId);
//
//        String token =
//                jwtService.generateToken(userDetails);
//
//        return new LoginResponse(
//
//                token,
//
//                user.getId(),
//
//                user.getUsername(),
//                userDetails.getClinicId(),
//                user.getRoles()
//                        .stream()
//                        .map(role ->
//                                role.getName().name()
//                        )
//                        .toList()
//        );
//    }



//    public LoginResponse login(LoginRequest request) {
//
//        AppUser user = userRepository
//                .findByUsername(request.username())
//                .orElse(null);
//
//        // Unknown username: reject without revealing whether it exists.
//        if (user == null) {
////            authenticationManager.authenticate(
////                    new UsernamePasswordAuthenticationToken(
////                            request.username(),
////                            request.password()
////                    )
////            );
//
//            throw new AccessDeniedException(
//                    "Invalid username or password"
//            );
//        }
//
//        // Reject accounts currently in the cooldown period.
//        loginAttemptService.checkLocked(user);
//
//        try {
//            authenticationManager.authenticate(
//                    new UsernamePasswordAuthenticationToken(
//                            request.username(),
//                            request.password()
//                    )
//            );
//        } catch (BadCredentialsException ex) {
//
//            // Must commit independently of the failed login response.
//            loginAttemptService.recordFailure(user.getId());
//
//            throw new AccessDeniedException(
//                    "Invalid username or password"
//            );
//        }
//
//        // Reset consecutive failures after successful authentication.
//        loginAttemptService.recordSuccess(user.getId());
//
//        boolean isSuperAdmin = user.getRoles().stream()
//                .anyMatch(role ->
//                        role.getName() == RoleName.SUPER_ADMIN
//                );
//
//        Long clinicId = clinicUserRepository
//                .findByUser_Id(user.getId())
//                .map(clinicUser -> roleClinicId(user, isSuperAdmin,
//                        clinicUser.getClinic().getId()))
//                .orElse(null);
//
//        if (!isSuperAdmin && clinicId == null) {
//            throw new AccessDeniedException(
//                    "Invalid username or password"
//            );
//        }
//
//        CustomUserDetails userDetails =
//                new CustomUserDetails(user, clinicId);
//
//        String token = jwtService.generateToken(userDetails);
//
//        return new LoginResponse(
//                token,
//                user.getId(),
//                user.getUsername(),
//                userDetails.getClinicId(),
//                user.getRoles()
//                        .stream()
//                        .map(role -> role.getName().name())
//                        .toList()
//        );
//    }
//
//    private Long roleClinicId(
//            AppUser user,
//            boolean isSuperAdmin,
//            Long assignedClinicId
//    ) {
//        // SUPER_ADMIN can log in without a clinic assignment.
//        return isSuperAdmin ? assignedClinicId : assignedClinicId;
//    }


    public AuthLoginResponse login(LoginRequest request) {

        AppUser user = userRepository
                .findByUsername(request.username())
                .orElse(null);

        // Keep the error generic for unknown usernames.
        if (user == null) {
            throw new BadCredentialsException(
                    "Invalid username or password"
            );
        }

        // Reject accounts in the cooldown period.
        loginAttemptService.checkLocked(user);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()
                    )
            );
        } catch (AuthenticationException ex) {

            loginAttemptService.recordFailure(user.getId());

            throw new BadCredentialsException(
                    "Invalid username or password"
            );
        }

        // Password is correct. Validate account/clinic configuration.
        boolean isSuperAdmin = user.getRoles().stream()
                .anyMatch(role ->
                        role.getName() == RoleName.SUPER_ADMIN
                );

        Long clinicId = clinicUserRepository
                .findByUser_Id(user.getId())
                .map(clinicUser -> clinicUser.getClinic().getId())
                .orElse(null);

        if (!isSuperAdmin && clinicId == null) {
            throw new BadCredentialsException(
                    "Invalid username or password"
            );
        }

        // Do not issue a normal access JWT before TOTP verification.
        if (user.isTotpEnabled()) {

            String challengeToken =
                    loginChallengeService.createChallenge(user.getId());

            return AuthLoginResponse.totpRequired(challengeToken);
        }

        // Complete login for accounts not requiring TOTP.
        loginAttemptService.recordSuccess(user.getId());

        CustomUserDetails userDetails =
                new CustomUserDetails(user, clinicId);

        String token = jwtService.generateToken(userDetails);

        return AuthLoginResponse.authenticated(
                new LoginResponse(
                        token,
                        user.getId(),
                        user.getUsername(),
                        userDetails.getClinicId(),
                        user.getRoles()
                                .stream()
                                .map(role -> role.getName().name())
                                .toList()
                )
        );
    }

    public AuthLoginResponse verifyTotpLogin(
            TotpLoginRequest request) {

        // This method must validate the challenge, verify TOTP,
        // and consume the challenge atomically.
        Long userId = loginChallengeService
                .verifyAndConsume(
                        request.challengeToken(),
                        request.code(),
                        totpService
                );

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Invalid login challenge"
                        ));

        boolean isSuperAdmin = user.getRoles().stream()
                .anyMatch(role ->
                        role.getName() == RoleName.SUPER_ADMIN
                );

        Long clinicId = clinicUserRepository
                .findByUser_Id(user.getId())
                .map(clinicUser ->
                        clinicUser.getClinic().getId())
                .orElse(null);

        if (!isSuperAdmin && clinicId == null) {
            throw new BadCredentialsException(
                    "Invalid login challenge"
            );
        }

        CustomUserDetails userDetails =
                new CustomUserDetails(user, clinicId);

        String token = jwtService.generateToken(userDetails);

        LoginResponse loginResponse = new LoginResponse(
                token,
                user.getId(),
                user.getUsername(),
                userDetails.getClinicId(),
                user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .toList()
        );

        loginAttemptService.recordSuccess(user.getId());

        return AuthLoginResponse.authenticated(loginResponse);
    }

    @Transactional
    public void enableTotp(Long userId, int code) {

        AppUser user = appUserRepository.findById(userId).orElseThrow(()-> new NotFoundException("User Not Found"));
        String pendingSecret = user.getPendingTotpSecret();

        if (pendingSecret == null ||
                !totpService.verifyCode(pendingSecret, code)) {
            throw new AccessDeniedException(
                    "Invalid authenticator code"
            );
        }

        user.setTotpSecret(pendingSecret);
        user.setPendingTotpSecret(null);
        user.setTotpEnabled(true);

        appUserRepository.save(user);
    }

    @Transactional
    public TotpSetupResponse setupTotp(Long userId) {

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new NotFoundException("User not found"));

        if (user.isTotpEnabled()) {
            throw new IllegalStateException(
                    "Authenticator is already enabled"
            );
        }

        String secret = totpService.generateSecret();

        user.setPendingTotpSecret(secret);
        userRepository.save(user);

        String otpauthUri = totpService.getProvisioningUri(
                user.getUsername(),
                secret
        );

        return new TotpSetupResponse(secret, otpauthUri);
    }

    @Transactional
    public void disableTotp(Long userId, TotpDisableRequest request) {

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new NotFoundException("User not found"));

        if (!user.isTotpEnabled() || user.getTotpSecret() == null) {
            throw new IllegalStateException(
                    "Authenticator is not enabled");
        }

        // Verify current password
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        user.getUsername(),
                        request.password()
                )
        );

        // Verify current authenticator code
        boolean validCode = totpService.verifyCode(
                user.getTotpSecret(),
                Integer.parseInt(request.code())
        );

        if (!validCode) {
            throw new BadCredentialsException(
                    "Invalid authenticator code");
        }

        // Disable authenticator and remove stored secrets
        user.setTotpEnabled(false);
        user.setTotpSecret(null);
        user.setPendingTotpSecret(null);

        userRepository.save(user);
    }

}
