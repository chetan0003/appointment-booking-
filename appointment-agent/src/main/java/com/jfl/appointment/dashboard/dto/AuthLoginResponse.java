package com.jfl.appointment.dashboard.dto;



public record AuthLoginResponse(
        boolean authenticated,
        boolean totpRequired,
        String challengeToken,
        LoginResponse login
) {

    public static AuthLoginResponse totpRequired(
            String challengeToken) {
        return new AuthLoginResponse(
                false,
                true,
                challengeToken,
                null
        );
    }

    public static AuthLoginResponse authenticated(
            LoginResponse login) {
        return new AuthLoginResponse(
                true,
                false,
                null,
                login
        );
    }
}

