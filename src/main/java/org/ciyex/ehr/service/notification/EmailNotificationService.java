package org.ciyex.ehr.service.notification;

import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.client.CommServiceClient;
import org.springframework.stereotype.Service;

/**
 * Email notification service.
 * Delegates to ciyex-comm for actual email delivery.
 */
@Service
@Slf4j
public class EmailNotificationService {

    private final CommServiceClient commServiceClient;

    public EmailNotificationService(CommServiceClient commServiceClient) {
        this.commServiceClient = commServiceClient;
    }

    public void sendEmail(String to, String subject, String body) {
        try {
            commServiceClient.sendEmail(to, subject, body);
            log.info("Email sent successfully to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}", to, e);
            throw new RuntimeException("Email sending failed", e);
        }
    }
}
