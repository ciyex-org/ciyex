package org.ciyex.ehr.service.notification;

import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.client.CommServiceClient;
import org.springframework.stereotype.Service;

/**
 * SMS notification service.
 * Delegates to ciyex-comm for actual SMS delivery.
 */
@Service
@Slf4j
public class SmsNotificationService {

    private final CommServiceClient commServiceClient;

    public SmsNotificationService(CommServiceClient commServiceClient) {
        this.commServiceClient = commServiceClient;
    }

    public void sendSms(String to, String body) {
        try {
            commServiceClient.sendSms(to, body);
            log.info("SMS sent successfully to {}", to);
        } catch (Exception e) {
            log.error("Failed to send SMS to {}", to, e);
            throw new RuntimeException("SMS sending failed", e);
        }
    }
}
