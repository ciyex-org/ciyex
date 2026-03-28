package org.ciyex.ehr.service.portal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.messaging.entity.Message;
import org.ciyex.ehr.messaging.repository.ChannelRepository;
import org.ciyex.ehr.messaging.repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import org.ciyex.ehr.messaging.entity.Channel;

/**
 * Posts structured notification messages to system channels when patients
 * make changes through the portal (demographics, insurance, documents, etc.).
 *
 * Messages are posted to pre-created system channels:
 *   - "patient-updates" — demographics, insurance, profile changes
 *   - "portal-activity" — document uploads, consent forms, appointment requests
 *
 * All notifications are system messages with structured metadata for
 * rich rendering in the messaging UI.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortalNotificationService {

    private final ChannelRepository channelRepo;
    private final MessageRepository messageRepo;

    private static final String CHANNEL_PATIENT_UPDATES = "patient-updates";
    private static final String CHANNEL_PORTAL_ACTIVITY = "portal-activity";

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    // ==================== Demographics ====================

    @Transactional
    public void notifyDemographicsUpdate(String patientName, Map<String, String> changedFields) {
        if (changedFields == null || changedFields.isEmpty()) return;

        String content = buildChangeContent(patientName, "demographics", changedFields);
        Map<String, Object> metadata = Map.of(
                "patientName", patientName,
                "updateType", "demographics",
                "changedFields", changedFields
        );
        postToChannel(CHANNEL_PATIENT_UPDATES, content, "demographics_update", metadata);
    }

    // ==================== Profile ====================

    @Transactional
    public void notifyProfileUpdate(String patientName, Map<String, String> changedFields) {
        if (changedFields == null || changedFields.isEmpty()) return;

        String content = buildChangeContent(patientName, "profile", changedFields);
        Map<String, Object> metadata = Map.of(
                "patientName", patientName,
                "updateType", "profile",
                "changedFields", changedFields
        );
        postToChannel(CHANNEL_PATIENT_UPDATES, content, "profile_update", metadata);
    }

    // ==================== Insurance ====================

    @Transactional
    public void notifyInsuranceUpdate(String patientName, String level, String action, Map<String, String> details) {
        String content = String.format("**%s** %s %s insurance coverage", patientName, action, level);
        if (details != null && !details.isEmpty()) {
            content += "\n" + formatDetails(details);
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("patientName", patientName);
        metadata.put("updateType", "insurance");
        metadata.put("level", level);
        metadata.put("action", action);
        if (details != null) metadata.put("details", details);

        postToChannel(CHANNEL_PATIENT_UPDATES, content, "insurance_update", metadata);
    }

    // ==================== Documents ====================

    @Transactional
    public void notifyDocumentUpload(String patientName, String fileName, String category) {
        String content = String.format("**%s** uploaded a document: *%s*", patientName, fileName);
        if (category != null) content += " (Category: " + category + ")";

        Map<String, Object> metadata = Map.of(
                "patientName", patientName,
                "updateType", "document_upload",
                "fileName", fileName,
                "category", category != null ? category : "uncategorized"
        );
        postToChannel(CHANNEL_PORTAL_ACTIVITY, content, "document_upload", metadata);
    }

    // ==================== Consent ====================

    @Transactional
    public void notifyConsentSigned(String patientName, String formName) {
        String content = String.format("**%s** signed consent form: *%s*", patientName, formName);

        Map<String, Object> metadata = Map.of(
                "patientName", patientName,
                "updateType", "consent_signed",
                "formName", formName
        );
        postToChannel(CHANNEL_PORTAL_ACTIVITY, content, "consent_signed", metadata);
    }

    // ==================== Review Submissions ====================

    @Transactional
    public void notifyReviewSubmitted(String patientName, String updateType, String hint) {
        String content = String.format("**%s** submitted a change for review: %s", patientName,
                hint != null ? hint : updateType);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("patientName", patientName);
        metadata.put("updateType", "review_submitted");
        metadata.put("reviewType", updateType);
        if (hint != null) metadata.put("hint", hint);

        postToChannel(CHANNEL_PORTAL_ACTIVITY, content, "review_submitted", metadata);
    }

    // ==================== Appointment Requests ====================

    @Transactional
    public void notifyAppointmentRequest(String patientName, String appointmentType, String preferredDate) {
        String content = String.format("**%s** requested an appointment: %s", patientName, appointmentType);
        if (preferredDate != null) content += " (Preferred: " + preferredDate + ")";

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("patientName", patientName);
        metadata.put("updateType", "appointment_request");
        metadata.put("appointmentType", appointmentType);
        if (preferredDate != null) metadata.put("preferredDate", preferredDate);

        postToChannel(CHANNEL_PORTAL_ACTIVITY, content, "appointment_request", metadata);
    }

    // ==================== Portal Registration ====================

    @Transactional
    public void notifyNewRegistration(String patientName, String email) {
        String content = String.format("**%s** (%s) registered on the patient portal", patientName, email);

        Map<String, Object> metadata = Map.of(
                "patientName", patientName,
                "updateType", "registration",
                "email", email
        );
        postToChannel(CHANNEL_PORTAL_ACTIVITY, content, "registration", metadata);
    }

    // ==================== Internal ====================

    private void postToChannel(String channelName, String content, String notificationType,
                               Map<String, Object> metadata) {
        try {
            String org = orgAlias();
            if (org == null) {
                log.warn("No org_alias in context, skipping portal notification");
                return;
            }

            Optional<Channel> channel = channelRepo.findByOrgAliasAndNameAndArchivedFalse(org, channelName);
            if (channel.isEmpty()) {
                log.debug("System channel '{}' not found for org '{}', skipping notification", channelName, org);
                return;
            }

            Message msg = Message.builder()
                    .channelId(channel.get().getId())
                    .senderId("portal-system")
                    .senderName("Patient Portal")
                    .content(content)
                    .system(true)
                    .systemType("portal_notification")
                    .notificationType(notificationType)
                    .metadata(metadata != null ? metadata : Map.of())
                    .orgAlias(org)
                    .build();
            messageRepo.save(msg);

            log.debug("Portal notification posted to #{}: {}", channelName, notificationType);

        } catch (Exception e) {
            // Never let notification failures break the main flow
            log.error("Failed to post portal notification to #{}: {}", channelName, e.getMessage(), e);
        }
    }

    private String buildChangeContent(String patientName, String section, Map<String, String> changedFields) {
        StringBuilder sb = new StringBuilder();
        sb.append("**").append(patientName).append("** updated ").append(section).append(":\n");
        for (Map.Entry<String, String> entry : changedFields.entrySet()) {
            sb.append("- ").append(formatFieldName(entry.getKey())).append(": ").append(entry.getValue()).append("\n");
        }
        return sb.toString().trim();
    }

    private String formatDetails(Map<String, String> details) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : details.entrySet()) {
            sb.append("- ").append(formatFieldName(entry.getKey())).append(": ").append(entry.getValue()).append("\n");
        }
        return sb.toString().trim();
    }

    private String formatFieldName(String camelCase) {
        // Convert camelCase to Title Case (e.g., "addressLine1" → "Address Line 1")
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append(' ');
            }
            if (i == 0) {
                sb.append(Character.toUpperCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
