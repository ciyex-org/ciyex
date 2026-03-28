package org.ciyex.ehr.messaging.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.messaging.entity.ChannelMember;
import org.ciyex.ehr.messaging.repository.ChannelMemberRepository;
import org.ciyex.ehr.notification.repository.NotificationPreferenceRepository;
import org.ciyex.ehr.notification.repository.NotificationTemplateRepository;
import org.ciyex.ehr.notification.service.NotificationService;
import org.ciyex.ehr.service.KeycloakUserService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sends email/SMS notifications to channel members when a new secure message is received.
 * Uses the notification framework (templates, preferences, notification_log) for delivery.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecureMessageNotificationService {

    private final NotificationService notificationService;
    private final NotificationPreferenceRepository prefRepo;
    private final NotificationTemplateRepository templateRepo;
    private final ChannelMemberRepository memberRepo;
    private final KeycloakUserService keycloakUserService;

    private static final String EVENT_TYPE = "secure_message_received";
    private static final String TEMPLATE_KEY = "secure_message_notification";

    /**
     * Notify all channel members (except the sender) about a new message.
     * Called asynchronously after a message is saved.
     *
     * @param orgAlias   The org alias for multi-tenant context
     * @param channelId  The channel where the message was sent
     * @param senderId   The sender's user ID (will be excluded from notifications)
     * @param senderName The sender's display name
     * @param content    The message content (truncated for notification)
     * @param channelName The channel name for context in the notification
     */
    @Async
    public void onMessageSent(String orgAlias, UUID channelId, String senderId,
                               String senderName, String content, String channelName) {
        try {
            // Check if secure_message_received event is enabled (org-specific or __SYSTEM__ fallback)
            var prefOpt = prefRepo.findByOrgAliasAndEventType(orgAlias, EVENT_TYPE);
            if (prefOpt.isEmpty()) {
                prefOpt = prefRepo.findByOrgAliasAndEventType("__SYSTEM__", EVENT_TYPE);
            }
            if (prefOpt.isEmpty()) {
                log.debug("No preference for '{}' in org '{}', skipping notification", EVENT_TYPE, orgAlias);
                return;
            }
            var pref = prefOpt.get();
            if (!Boolean.TRUE.equals(pref.getEmailEnabled()) && !Boolean.TRUE.equals(pref.getSmsEnabled())) {
                log.debug("Both email and SMS disabled for '{}' in org '{}'", EVENT_TYPE, orgAlias);
                return;
            }

            // Get all channel members except the sender
            List<ChannelMember> members = memberRepo.findByChannelIdOrderByDisplayNameAsc(channelId);
            List<ChannelMember> recipients = members.stream()
                    .filter(m -> !m.getUserId().equals(senderId))
                    .toList();

            if (recipients.isEmpty()) {
                log.debug("No recipients to notify for message in channel {}", channelId);
                return;
            }

            // Truncate content for notification preview
            String preview = content.length() > 200 ? content.substring(0, 200) + "..." : content;

            // Try to find a template
            var templateOpt = templateRepo.findByOrgAliasAndTemplateKeyAndChannelType(
                    orgAlias, TEMPLATE_KEY, "email");

            String subject;
            String body;
            if (templateOpt.isPresent() && Boolean.TRUE.equals(templateOpt.get().getIsActive())) {
                var template = templateOpt.get();
                subject = replaceVariables(template.getSubject(), senderName, preview, channelName);
                body = replaceVariables(template.getBody(), senderName, preview, channelName);
            } else {
                // Default template
                subject = "New secure message from " + senderName;
                body = buildDefaultEmailBody(senderName, preview, channelName);
            }

            // Send to each recipient — resolve Keycloak UUID to email first
            for (ChannelMember recipient : recipients) {
                try {
                    if (Boolean.TRUE.equals(pref.getEmailEnabled())) {
                        String recipientEmail = resolveUserEmail(recipient.getUserId());
                        if (recipientEmail == null || recipientEmail.isBlank()) {
                            log.warn("Could not resolve email for userId {}, skipping notification",
                                    recipient.getUserId());
                            continue;
                        }
                        notificationService.send(orgAlias, "email",
                                recipientEmail, subject, body, null, "auto_secure_message");
                        log.info("Sent secure message notification to {} ({}) in org {}",
                                recipient.getDisplayName(), recipientEmail, orgAlias);
                    }
                } catch (Exception e) {
                    log.warn("Failed to notify {} about secure message: {}",
                            recipient.getDisplayName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to send secure message notifications for channel {} in org {}: {}",
                    channelId, orgAlias, e.getMessage(), e);
        }
    }

    /**
     * Resolve a Keycloak user ID to their email address.
     * If the userId is already an email, returns it as-is.
     */
    private String resolveUserEmail(String userId) {
        if (userId == null || userId.isBlank()) return null;
        // Already an email
        if (userId.contains("@")) return userId;
        // Look up in Keycloak
        try {
            var user = keycloakUserService.getUserById(userId);
            if (user != null && user.getEmail() != null && !user.getEmail().isBlank()) {
                return user.getEmail();
            }
        } catch (Exception e) {
            log.warn("Failed to resolve email for userId {}: {}", userId, e.getMessage());
        }
        return null;
    }

    private String replaceVariables(String template, String senderName, String preview, String channelName) {
        return template
                .replace("{{sender_name}}", senderName)
                .replace("{{message_preview}}", preview)
                .replace("{{channel_name}}", channelName != null ? channelName : "Direct Message");
    }

    private String buildDefaultEmailBody(String senderName, String preview, String channelName) {
        String channelDisplay = channelName != null ? channelName : "Direct Message";
        return "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;\">"
                + "<h2 style=\"color: #1a1a1a;\">New Secure Message</h2>"
                + "<p style=\"color: #555;\">You have a new message from <strong>" + senderName + "</strong>"
                + " in <strong>" + channelDisplay + "</strong>:</p>"
                + "<div style=\"background: #f5f5f5; border-left: 4px solid #4f46e5; padding: 12px 16px; "
                + "margin: 16px 0; border-radius: 4px;\">"
                + "<p style=\"color: #333; margin: 0;\">" + preview + "</p>"
                + "</div>"
                + "<p style=\"color: #555;\">Log in to your account to view the full message and reply securely.</p>"
                + "<hr style=\"border: none; border-top: 1px solid #eee; margin: 24px 0;\">"
                + "<p style=\"color: #999; font-size: 12px;\">This is an automated notification. "
                + "Please do not reply to this email. All messages should be sent through the secure messaging system.</p>"
                + "</div>";
    }
}
