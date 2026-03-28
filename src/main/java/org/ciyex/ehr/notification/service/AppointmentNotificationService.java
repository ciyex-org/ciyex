package org.ciyex.ehr.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.notification.repository.NotificationPreferenceRepository;
import org.ciyex.ehr.notification.repository.NotificationTemplateRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Handles sending notifications triggered by appointment events.
 * Called from FhirFacadeController after appointment creation/update.
 * NOTE: Patient email must be resolved BEFORE calling @Async methods
 * (pass patientEmail in appointmentData) since FHIR/security context
 * is not available in async threads.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentNotificationService {

    private final NotificationService notificationService;
    private final NotificationPreferenceRepository prefRepo;
    private final NotificationTemplateRepository templateRepo;

    /**
     * Fire notification after an appointment is created (booked).
     * Checks event preferences and sends email if appointment_confirmation is enabled.
     */
    @Async
    public void onAppointmentCreated(String orgAlias, Map<String, Object> appointmentData) {
        try {
            sendEventNotification(orgAlias, "appointment_confirmation", appointmentData);
        } catch (Exception e) {
            log.error("Failed to send appointment confirmation notification for org {}: {}",
                    orgAlias, e.getMessage(), e);
        }
    }

    /**
     * Fire notification for appointment reminder.
     */
    @Async
    public void onAppointmentReminder(String orgAlias, Map<String, Object> appointmentData) {
        try {
            sendEventNotification(orgAlias, "appointment_reminder", appointmentData);
        } catch (Exception e) {
            log.error("Failed to send appointment reminder for org {}: {}",
                    orgAlias, e.getMessage(), e);
        }
    }

    private void sendEventNotification(String orgAlias, String eventType, Map<String, Object> data) {
        // Check if this event type is enabled for email
        var prefOpt = prefRepo.findByOrgAliasAndEventType(orgAlias, eventType);
        if (prefOpt.isEmpty()) {
            log.debug("No preference found for event '{}' in org '{}', skipping", eventType, orgAlias);
            return;
        }

        var pref = prefOpt.get();
        if (!Boolean.TRUE.equals(pref.getEmailEnabled())) {
            log.debug("Email disabled for event '{}' in org '{}', skipping", eventType, orgAlias);
            return;
        }

        // Get patient info from appointment data (email must be resolved by caller)
        String patientEmail = extractString(data, "patientEmail");
        String patientName = extractString(data, "patientName");
        Long patientId = extractLong(data, "patientId");

        if (patientEmail == null || patientEmail.isBlank()) {
            log.info("No patient email found for notification (event={}, patientId={}), skipping", eventType, patientId);
            return;
        }

        // Build template variables
        Map<String, String> variables = Map.of(
                "patientName", patientName != null ? patientName : "",
                "providerName", extractString(data, "providerName", ""),
                "appointmentDate", extractString(data, "startDate", ""),
                "appointmentTime", extractString(data, "startTime", ""),
                "practiceName", extractString(data, "practiceName", orgAlias),
                "practicePhone", extractString(data, "practicePhone", ""),
                "portalLink", ""
        );

        // Try to send using template first
        String templateKey = eventType;
        var templateOpt = templateRepo.findByOrgAliasAndTemplateKeyAndChannelType(
                orgAlias, templateKey, "email");

        if (templateOpt.isPresent() && Boolean.TRUE.equals(templateOpt.get().getIsActive())) {
            try {
                String templateSubject = resolveVars(templateOpt.get().getSubject(), variables);
                String templateBody = resolveVars(templateOpt.get().getBody(), variables);
                // Use default subject if template subject is empty
                if (templateSubject == null || templateSubject.isBlank()) {
                    templateSubject = buildDefaultSubject(eventType, patientName);
                }
                notificationService.send(
                        orgAlias, "email", patientEmail,
                        templateSubject,
                        templateBody,
                        patientId, "auto_" + eventType);
                log.info("Sent {} email to {} for org {}", eventType, patientEmail, orgAlias);
                return;
            } catch (Exception e) {
                log.warn("Template-based send failed for {}: {}", eventType, e.getMessage());
            }
        }

        // Fallback: send with default subject/body
        String subject = buildDefaultSubject(eventType, patientName);
        String body = buildDefaultBody(eventType, variables);
        notificationService.send(orgAlias, "email", patientEmail, subject, body, patientId, "auto_" + eventType);
        log.info("Sent {} email (default template) to {} for org {}", eventType, patientEmail, orgAlias);
    }

    private String resolveVars(String template, Map<String, String> variables) {
        if (template == null) return "";
        String result = template;
        for (var entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    private String buildDefaultSubject(String eventType, String patientName) {
        return switch (eventType) {
            case "appointment_confirmation" -> "Appointment Confirmation" +
                    (patientName != null ? " for " + patientName : "");
            case "appointment_reminder" -> "Appointment Reminder" +
                    (patientName != null ? " for " + patientName : "");
            case "lab_result_ready" -> "Lab Results Available";
            case "prescription_ready" -> "Prescription Ready for Pickup";
            default -> "Notification from Your Healthcare Provider";
        };
    }

    private String buildDefaultBody(String eventType, Map<String, String> vars) {
        String name = vars.getOrDefault("patientName", "Patient");
        String provider = vars.getOrDefault("providerName", "your provider");
        String date = vars.getOrDefault("appointmentDate", "");
        String time = vars.getOrDefault("appointmentTime", "");
        String practice = vars.getOrDefault("practiceName", "");

        return switch (eventType) {
            case "appointment_confirmation" -> String.format(
                    "<p>Dear %s,</p><p>Your appointment with %s has been confirmed for %s at %s.</p>" +
                    "<p>Practice: %s</p><p>Thank you!</p>",
                    name, provider, date, time, practice);
            case "appointment_reminder" -> String.format(
                    "<p>Dear %s,</p><p>This is a reminder about your upcoming appointment with %s on %s at %s.</p>" +
                    "<p>Practice: %s</p><p>Thank you!</p>",
                    name, provider, date, time, practice);
            default -> String.format("<p>Dear %s,</p><p>You have a new notification from %s.</p>", name, practice);
        };
    }

    private String extractString(Map<String, Object> data, String key) {
        return extractString(data, key, null);
    }

    private String extractString(Map<String, Object> data, String key, String defaultVal) {
        Object val = data.get(key);
        return val != null ? String.valueOf(val) : defaultVal;
    }

    private Long extractLong(Map<String, Object> data, String key) {
        Object val = data.get(key);
        if (val == null) return null;
        if (val instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(val)); } catch (Exception e) { return null; }
    }
}
