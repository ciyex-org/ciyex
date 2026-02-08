package org.ciyex.ehr.service.notification;

import org.ciyex.ehr.service.OrgConfigService;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
@Slf4j

public class EmailNotificationService {
    private final OrgConfigService orgConfigService;

    public EmailNotificationService(OrgConfigService orgConfigService) {
        this.orgConfigService = orgConfigService;
    }

    public void sendEmail(String to, String subject, String body) {
        // Fetch SMTP config from org_config table
        String host = orgConfigService.getConfig("smtp.server").orElse("smtp.sendgrid.net");
        int port = orgConfigService.getConfig("smtp.port").map(Integer::parseInt).orElse(587);
        String username = orgConfigService.getConfig("smtp.username").orElse("");
        String password = orgConfigService.getConfig("smtp.password").orElse("");
        String fromAddress = orgConfigService.getConfig("smtp.fromAddress").orElse("notification@ciyex.com");
        String fromName = orgConfigService.getConfig("smtp.fromName").orElse("Ciyex Health");

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress, fromName));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
            log.info("✅ Email sent successfully to {}", to);
        } catch (Exception e) {
            log.error("❌ Failed to send email to {}", to, e);
            throw new RuntimeException("Email sending failed", e);
        }
    }
}
