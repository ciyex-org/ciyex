package com.qiaben.ciyex.service.notification;

import com.qiaben.ciyex.dto.integration.IntegrationKey;
import com.qiaben.ciyex.dto.integration.SmtpConfig;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import com.qiaben.ciyex.util.TenantContextUtil;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
@Slf4j
public class EmailNotificationService {

    private final OrgIntegrationConfigProvider configProvider;

    public EmailNotificationService(OrgIntegrationConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    public void sendEmail(String to, String subject, String body) {
        String tenantName = TenantContextUtil.getTenantName();
        SmtpConfig smtp = configProvider.getForCurrentTenant(IntegrationKey.SMTP);
        // Log helpful context
        if (tenantName == null) {
            log.warn("Email send attempted with no tenantName in context");
        }

        // --- Apply safe defaults if DB has only server/username/password ---
        String host = smtp.getServer() != null ? smtp.getServer() : "smtp.sendgrid.net";
        int port = (smtp.getPort() != null) ? smtp.getPort() : 587;   // ✅ fixed
        String fromAddress = (smtp.getFromAddress() != null && !smtp.getFromAddress().isBlank())
                ? smtp.getFromAddress()
                : "notification@ciyex.com";  // fallback default
        String fromName = (smtp.getFromName() != null && !smtp.getFromName().isBlank())
                ? smtp.getFromName()
                : "Qiaben Health";  // fallback default

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtp.getUsername(), smtp.getPassword());
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
