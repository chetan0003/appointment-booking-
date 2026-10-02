package com.jfl.appointment.service;


import com.jfl.appointment.util.Constants;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class WhatsAppQrLinkService {

    public String generateLink(
            Long clinicId,
            String whatsappNumber,
            String token,
            boolean isClinicQr
    ) {

        String message = null;
        if (isClinicQr) {
            message = Constants.HOLA_MD_HEADER+ ":" + clinicId + ":" + token + ":" + Constants.QR_CODE_TYPE;
        } else {
            message = "Hi HOLA_MD:" + clinicId + ":" + token;
        }

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
