package com.jfl.appointment.dashboard.service;


import com.jfl.appointment.repository.AppUserRepository;
import com.jfl.appointment.repository.LoginChallengeRepository;
import com.jfl.appointment.util.Constants;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TotpService {

    private final GoogleAuthenticator authenticator =
            new GoogleAuthenticator();


    public String generateSecret() {
        GoogleAuthenticatorKey key =
                authenticator.createCredentials();

        return key.getKey();
    }

    public boolean verifyCode(String secret, int code) {
        if (secret == null || code < 0 || code > 999999) {
            return false;
        }

        return authenticator.authorize(secret, code);
    }

    public String getProvisioningUri(
            String username,
            String secret
    ) {
        String issuer = Constants.HOLA_MD;

        return "otpauth://totp/"
                + urlEncode(issuer + ":" + username)
                + "?secret=" + urlEncode(secret)
                + "&issuer=" + urlEncode(issuer)
                + "&algorithm=SHA1&digits=6&period=30";
    }

    private String urlEncode(String value) {
        return java.net.URLEncoder
                .encode(
                        value,
                        java.nio.charset.StandardCharsets.UTF_8
                )
                .replace("+", "%20");
    }

}

