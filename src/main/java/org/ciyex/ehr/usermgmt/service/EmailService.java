package org.ciyex.ehr.usermgmt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.notification.entity.NotificationConfig;
import org.ciyex.ehr.notification.repository.NotificationConfigRepository;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.Properties;

/**
 * Sends emails using practice-specific SMTP config from the notification_config table.
 * Each practice must configure their own SMTP settings — there is no global fallback.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final NotificationConfigRepository configRepo;
    private final ObjectMapper objectMapper;

    /**
     * Send an email using the org's configured SMTP.
     */
    public void sendEmail(String to, String subject, String htmlBody) {
        SenderContext ctx = buildSenderContext(null);
        doSend(ctx, to, subject, htmlBody);
    }

    /**
     * Send an email with explicit orgAlias — safe to call from @Async threads.
     */
    public void sendEmail(String orgAlias, String to, String subject, String htmlBody) {
        SenderContext ctx = buildSenderContext(orgAlias);
        doSend(ctx, to, subject, htmlBody);
    }

    private void doSend(SenderContext ctx, String to, String subject, String htmlBody) {
        try {
            MimeMessage message = ctx.mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            // Use configured sender_address + sender_name, fallback to SMTP username
            String fromAddress = ctx.senderAddress != null ? ctx.senderAddress : ctx.mailSender.getUsername();
            if (ctx.senderName != null && !ctx.senderName.isBlank()) {
                helper.setFrom(fromAddress, ctx.senderName);
            } else {
                helper.setFrom(fromAddress);
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            ctx.mailSender.send(message);
            log.info("Email sent to {} from {} with subject '{}'", to, fromAddress, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /** Holds the JavaMailSender plus sender identity from notification_config. */
    private record SenderContext(JavaMailSenderImpl mailSender, String senderAddress, String senderName) {}

    private SenderContext buildSenderContext(String orgAlias) {
        String org;
        try {
            org = orgAlias != null ? orgAlias : RequestContext.get().getOrgName();
        } catch (Exception e) {
            throw new RuntimeException("Cannot determine org for SMTP config — no orgAlias provided and no RequestContext available");
        }

        var configOpt = configRepo.findByOrgAliasAndChannelType(org, "email");
        if (configOpt.isPresent() && Boolean.TRUE.equals(configOpt.get().getEnabled())) {
            NotificationConfig nc = configOpt.get();
            JavaMailSenderImpl sender = buildFromNotificationConfig(nc);
            return new SenderContext(sender, nc.getSenderAddress(), nc.getSenderName());
        }

        throw new RuntimeException("No SMTP email configuration found for practice '" + org
                + "'. Configure SMTP in Settings > Notifications.");
    }

    @SuppressWarnings("unchecked")
    private JavaMailSenderImpl buildFromNotificationConfig(NotificationConfig config) {
        try {
            Map<String, Object> smtpConfig = objectMapper.readValue(config.getConfig(), Map.class);
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost((String) smtpConfig.getOrDefault("host", "smtp.office365.com"));
            Object portVal = smtpConfig.getOrDefault("port", 587);
            sender.setPort(portVal instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(portVal)));
            sender.setUsername((String) smtpConfig.getOrDefault("username", config.getSenderAddress()));
            sender.setPassword((String) smtpConfig.get("password"));

            Properties props = sender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable",
                    String.valueOf(smtpConfig.getOrDefault("use_tls", true)));
            props.put("mail.smtp.connectiontimeout", "10000");
            props.put("mail.smtp.timeout", "10000");

            return sender;
        } catch (Exception e) {
            log.error("Failed to parse SMTP config for practice '{}': {}", config.getOrgAlias(), e.getMessage(), e);
            throw new RuntimeException("Invalid SMTP configuration for practice '" + config.getOrgAlias() + "'", e);
        }
    }
}
