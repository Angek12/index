package com.index.platform.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/** Sends SMS (Africa's Talking) and WhatsApp (Twilio). Without keys it only logs, so nothing breaks. */
@Service
public class MessageGateway {
    private static final Logger log = LoggerFactory.getLogger(MessageGateway.class);
    private final RestClient http = RestClient.create();
    private final String atUser, atKey, atSender, atBase, twSid, twToken, twFrom;

    public MessageGateway(@Value("${app.sms.at-username:}") String atUser,
                          @Value("${app.sms.at-api-key:}") String atKey,
                          @Value("${app.sms.at-sender:}") String atSender,
                          @Value("${app.sms.at-base-url:https://api.africastalking.com}") String atBase,
                          @Value("${app.whatsapp.twilio-sid:}") String twSid,
                          @Value("${app.whatsapp.twilio-token:}") String twToken,
                          @Value("${app.whatsapp.twilio-from:}") String twFrom) {
        this.atUser = atUser; this.atKey = atKey; this.atSender = atSender; this.atBase = atBase;
        this.twSid = twSid; this.twToken = twToken; this.twFrom = twFrom;
    }

    @Async
    public void sendSms(String to, String text) {
        if (atKey.isBlank() || atUser.isBlank()) {
            log.info("[SMS not configured] to {}: {}", to, text);
            return;
        }
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("username", atUser);
            form.add("to", to);
            form.add("message", text);
            if (!atSender.isBlank()) form.add("from", atSender);
            http.post().uri(atBase + "/version1/messaging")
                    .header("apiKey", atKey).header("Accept", "application/json")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form).retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.warn("SMS to {} failed: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendWhatsapp(String to, String text) {
        if (twSid.isBlank() || twToken.isBlank() || twFrom.isBlank()) {
            log.info("[WhatsApp not configured] to {}: {}", to, text);
            return;
        }
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("From", "whatsapp:" + twFrom);
            form.add("To", "whatsapp:" + to);
            form.add("Body", text);
            http.post().uri("https://api.twilio.com/2010-04-01/Accounts/" + twSid + "/Messages.json")
                    .headers(h -> h.setBasicAuth(twSid, twToken))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form).retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.warn("WhatsApp to {} failed: {}", to, e.getMessage());
        }
    }
}
