package com.jfl.appointment.dashboard.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.jfl.appointment.entity.Clinic;
import com.jfl.appointment.entity.ClinicWhatsAppConfig;
import com.jfl.appointment.entity.WhatsAppConfigStatus;
import com.jfl.appointment.entity.WhatsAppProvider;
import com.jfl.appointment.exception.NotFoundException;
import com.jfl.appointment.repository.ClinicRepository;
import com.jfl.appointment.repository.ClinicWhatsAppConfigRepository;
import com.jfl.appointment.util.AESUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppIntegrationService {

    private final RestClient restClient;
    private final ClinicRepository clinicRepository;
    private final ClinicWhatsAppConfigRepository clinicWhatsAppConfigRepository;

    @Value("${meta.app-id}")
    private String appId;

    @Value("${meta.app-secret}")
    private String appSecret;

    @Value("${meta.graph-api-url}")
    private String graphApiUrl;


    public void connectWhatsApp(String code, Long clinicId) {
        // Step 1: Exchange temporary auth code for User Access Token
        String userAccessToken = exchangeCodeForAccessToken(code);

        // Step 2: Fetch WABA ID AND Meta Business Account ID
        Map<String, String> metaAccountIds = fetchMetaAccountIds(userAccessToken);
        String wabaId = metaAccountIds.get("wabaId");
        String businessAccountId = metaAccountIds.get("businessAccountId");

        // Step 3: Fetch Phone Number ID under this WABA
        Map<String, String> phoneDetails = fetchPhoneNumberDetails(wabaId, userAccessToken);
        String phoneNumberId = phoneDetails.get("phoneNumberId");
        String displayPhoneNumber = phoneDetails.get("displayPhoneNumber");

        // Step 4: Subscribe Hola MD app to WABA Webhooks
        subscribeAppToWaba(wabaId, userAccessToken);

        // Step 5: Encrypt and persist all identifiers to database
        saveClinicCredentials(clinicId, wabaId, phoneNumberId, displayPhoneNumber, userAccessToken, businessAccountId);
    }

    private String exchangeCodeForAccessToken(String code) {
        JsonNode response = restClient.get()
                .uri(graphApiUrl + "/oauth/access_token?client_id={appId}&client_secret={appSecret}&code={code}",
                        appId, appSecret, code)
                .retrieve()
                .body(JsonNode.class);

        return response.get("access_token").asText();
    }


    private Map<String, String> fetchPhoneNumberDetails(String wabaId, String userAccessToken) {
        JsonNode response = restClient.get()
                .uri(graphApiUrl + "/{wabaId}/phone_numbers", wabaId)
                .header("Authorization", "Bearer " + userAccessToken)
                .retrieve()
                .body(JsonNode.class);

        JsonNode firstPhone = response.get("data").get(0);

        Map<String, String> details = new HashMap<>();
        details.put("phoneNumberId", firstPhone.get("id").asText());
        details.put("displayPhoneNumber", firstPhone.get("display_phone_number").asText());
        return details;
    }

    private Map<String, String> fetchMetaAccountIds(String userAccessToken) {
        String appAccessToken = appId + "|" + appSecret;

        JsonNode response = restClient.get()
                .uri(graphApiUrl + "/debug_token?input_token={userToken}&access_token={appToken}",
                        userAccessToken, appAccessToken)
                .retrieve()
                .body(JsonNode.class);

        JsonNode data = response.get("data");

        Map<String, String> accountIds = new HashMap<>();

        // 1. Extract WABA ID (WhatsApp Business Account ID)
        if (data.has("target_ids") && data.get("target_ids").isArray() && !data.get("target_ids").isEmpty()) {
            accountIds.put("wabaId", data.get("target_ids").get(0).asText());
        }

        // 2. Extract Meta Business Manager Account ID
        if (data.has("granter_id")) {
            // Option A: Provided directly as granter_id
            accountIds.put("businessAccountId", data.get("granter_id").asText());
        } else if (data.has("business_id")) {
            // Option B: Fallback field depending on Embedded Signup API version
            accountIds.put("businessAccountId", data.get("business_id").asText());
        }

        return accountIds;
    }

    private void subscribeAppToWaba(String wabaId, String userAccessToken) {
        restClient.post()
                .uri(graphApiUrl + "/{wabaId}/subscribed_apps", wabaId)
                .header("Authorization", "Bearer " + userAccessToken)
                .retrieve()
                .toBodilessEntity();
    }

    private void saveClinicCredentials(
            Long clinicId,
            String wabaId,
            String whatsappNumber,
            String twilioAccountSid,
            String twilioSubaccountSid,
            String twilioWhatsappSenderSid
    ) {

        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Clinic not found with id: " + clinicId
                        )
                );

        /*
         * Check whether WhatsApp is already configured
         * for this clinic.
         */
        ClinicWhatsAppConfig whatsAppConfig =
                clinicWhatsAppConfigRepository
                        .findByClinicId(clinicId)
                        .orElse(null);

        if (whatsAppConfig == null) {

            whatsAppConfig = new ClinicWhatsAppConfig();

            whatsAppConfig.setClinic(clinic);

        }

        /*
         * Clinic WhatsApp number.
         *
         * Example:
         * 919876543210
         */
        whatsAppConfig.setWhatsappNumber(
                whatsappNumber
        );

        /*
         * Current provider.
         */
        whatsAppConfig.setProvider(
                WhatsAppProvider.TWILIO
        );

        /*
         * Twilio configuration.
         */
        whatsAppConfig.setTwilioAccountSid(
                twilioAccountSid
        );

        whatsAppConfig.setTwilioSubaccountSid(
                twilioSubaccountSid
        );

        whatsAppConfig.setTwilioWhatsappSenderSid(
                twilioWhatsappSenderSid
        );

        /*
         * WhatsApp Business Account ID.
         */
        whatsAppConfig.setWabaId(wabaId);

        /*
         * Activate configuration.
         */
        whatsAppConfig.setStatus(
                WhatsAppConfigStatus.ACTIVE
        );

        /*
         * Save.
         *
         * @PrePersist / @PreUpdate will handle
         * createdAt and updatedAt.
         */
        clinicWhatsAppConfigRepository.save(
                whatsAppConfig
        );

        log.info(
                "WhatsApp credentials saved for Clinic: {} | WABA: {} | WhatsApp Number: {}",
                clinicId,
                wabaId,
                whatsappNumber
        );
    }
    private String fetchBusinessIdFromWaba(String wabaId, String userAccessToken) {
        JsonNode response = restClient.get()
                .uri(graphApiUrl + "/{wabaId}?fields=owner_business_info", wabaId)
                .header("Authorization", "Bearer " + userAccessToken)
                .retrieve()
                .body(JsonNode.class);

        if (response.has("owner_business_info")) {
            return response.get("owner_business_info").get("id").asText();
        }
        return null;
    }
}
