package com.jfl.appointment.service;


import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class WhatsAppQrLinkService {

    public String generateLink(
            String whatsappNumber,
            String token
    ) {

        String message =
                "Hi Hola MD " + token;

        String encodedMessage =
                URLEncoder.encode(
                        message,
                        StandardCharsets.UTF_8
                );

        return "https://wa.me/"
                + normalizePhoneNumber(whatsappNumber)
                + "?text="
                + encodedMessage;
    }

    private String normalizePhoneNumber(
            String phoneNumber
    ) {

        return phoneNumber
                .replace("+", "")
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "");
    }
}
