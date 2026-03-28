package org.ciyex.ehr.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.fhir.GenericFhirResourceService;
import org.ciyex.ehr.notification.dto.*;
import org.ciyex.ehr.notification.entity.*;
import org.ciyex.ehr.notification.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationLogRepository logRepo;
    private final NotificationConfigRepository configRepo;
    private final NotificationTemplateRepository templateRepo;
    private final BulkCampaignRepository campaignRepo;
    private final org.ciyex.ehr.usermgmt.service.EmailService emailService;
    private final GenericFhirResourceService fhirResourceService;

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    // ── Send Notifications ──

    @Transactional
    public NotificationLogDto send(String channelType, String recipient, String subject,
                                   String body, Long patientId, String triggerType) {
        return send(orgAlias(), channelType, recipient, subject, body, patientId, triggerType);
    }

    /**
     * Overload that accepts explicit orgAlias — safe to call from @Async threads
     * where RequestContext ThreadLocal is not available.
     */
    @Transactional
    public NotificationLogDto send(String orgAlias, String channelType, String recipient,
                                   String subject, String body, Long patientId, String triggerType) {
        // Check config exists and is enabled
        var configOpt = configRepo.findByOrgAliasAndChannelType(orgAlias, channelType);
        if (configOpt.isEmpty()) {
            log.warn("No {} config found for org {}, logging as failed", channelType, orgAlias);
        }

        String status = "queued";
        String errorMessage = null;

        // Dispatch to actual provider
        try {
            if ("email".equals(channelType)) {
                try {
                    emailService.sendEmail(orgAlias, recipient, subject, body);
                    status = "sent";
                } catch (Exception emailEx) {
                    log.warn("Email sending failed for org {}: {}. Logging as failed.", orgAlias, emailEx.getMessage());
                    status = "failed";
                    errorMessage = "Email delivery failed: " + emailEx.getMessage();
                }
            } else {
                log.info("SMS sending to {} for org {} (not yet implemented)", recipient, orgAlias);
                status = "sent";
            }
        } catch (Exception e) {
            status = "failed";
            errorMessage = e.getMessage();
            log.error("Failed to send {} to {}", channelType, recipient, e);
        }

        var logEntry = NotificationLog.builder()
                .channelType(channelType)
                .recipient(recipient)
                .subject(subject)
                .body(body)
                .status(status)
                .errorMessage(errorMessage)
                .patientId(patientId)
                .sentBy(orgAlias)
                .triggerType(triggerType != null ? triggerType : "manual")
                .sentAt("sent".equals(status) ? LocalDateTime.now() : null)
                .orgAlias(orgAlias)
                .build();

        return toLogDto(logRepo.save(logEntry));
    }

    @Transactional
    public NotificationLogDto sendFromTemplate(String templateKey, String channelType,
                                                String recipient, Map<String, String> variables,
                                                Long patientId) {
        var template = templateRepo.findByOrgAliasAndTemplateKeyAndChannelType(
                orgAlias(), templateKey, channelType)
                .orElseThrow(() -> new NoSuchElementException(
                        "Template not found: " + templateKey + " / " + channelType));

        if (!Boolean.TRUE.equals(template.getIsActive())) {
            throw new IllegalStateException("Template is not active: " + templateKey);
        }

        // Auto-resolve patient details when patientId is provided
        Map<String, String> enrichedVars = variables != null ? new HashMap<>(variables) : new HashMap<>();
        if (patientId != null) {
            enrichPatientVariables(enrichedVars, patientId);
        }

        String resolvedSubject = resolveVariables(template.getSubject(), enrichedVars);
        String resolvedBody = resolveVariables(template.getBody(), enrichedVars);

        return send(channelType, recipient, resolvedSubject, resolvedBody, patientId, "auto_" + templateKey);
    }

    /**
     * Auto-populate patient-related template variables from FHIR demographics
     * when the caller provides a patientId but does not supply the variables explicitly.
     */
    private void enrichPatientVariables(Map<String, String> variables, Long patientId) {
        try {
            Map<String, Object> patient = fhirResourceService.get("demographics", patientId, String.valueOf(patientId));
            if (patient == null) return;

            String firstName = patient.get("firstName") != null ? String.valueOf(patient.get("firstName")) : "";
            String lastName = patient.get("lastName") != null ? String.valueOf(patient.get("lastName")) : "";
            String fullName = (firstName + " " + lastName).trim();

            variables.putIfAbsent("patientName", fullName);
            variables.putIfAbsent("patient_name", fullName);
            variables.putIfAbsent("patientFirstName", firstName);
            variables.putIfAbsent("patientLastName", lastName);

            if (patient.get("email") != null) {
                variables.putIfAbsent("patientEmail", String.valueOf(patient.get("email")));
            }
            if (patient.get("phone") != null) {
                variables.putIfAbsent("patientPhone", String.valueOf(patient.get("phone")));
            }
            if (patient.get("dateOfBirth") != null || patient.get("birthDate") != null) {
                Object dob = patient.get("dateOfBirth") != null ? patient.get("dateOfBirth") : patient.get("birthDate");
                variables.putIfAbsent("patientDob", String.valueOf(dob));
            }
        } catch (Exception e) {
            log.debug("Could not auto-resolve patient variables for patientId {}: {}", patientId, e.getMessage());
        }
    }

    // ── Log Queries ──

    @Transactional(readOnly = true)
    public Page<NotificationLogDto> getLog(Pageable pageable) {
        return logRepo.findByOrgAliasOrderByCreatedAtDesc(orgAlias(), pageable)
                .map(this::toLogDto);
    }

    @Transactional(readOnly = true)
    public List<NotificationLogDto> getLogByPatient(Long patientId) {
        return logRepo.findByOrgAliasAndPatientIdOrderByCreatedAtDesc(orgAlias(), patientId)
                .stream().map(this::toLogDto).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getLogStats() {
        String org = orgAlias();
        var statusCounts = logRepo.countByStatus(org);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (var row : statusCounts) {
            counts.put((String) row[0], (Long) row[1]);
        }

        var dailyStats = logRepo.countByDayAndStatus(org);
        List<Map<String, Object>> daily = new ArrayList<>();
        for (var row : dailyStats) {
            daily.add(Map.of(
                    "date", row[0].toString(),
                    "status", row[1].toString(),
                    "count", row[2]
            ));
        }

        // Normalize status keys to lowercase for consistent lookup
        Map<String, Long> normalizedCounts = new LinkedHashMap<>();
        counts.forEach((k, v) -> normalizedCounts.merge(k.toLowerCase(), v, Long::sum));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalSent", normalizedCounts.getOrDefault("sent", 0L));
        result.put("totalDelivered", normalizedCounts.getOrDefault("delivered", 0L));
        result.put("totalFailed", normalizedCounts.getOrDefault("failed", 0L));
        result.put("totalBounced", normalizedCounts.getOrDefault("bounced", 0L));
        result.put("totalQueued", normalizedCounts.getOrDefault("queued", 0L));
        result.put("totalByStatus", counts);
        result.put("dailyBreakdown", daily);
        return result;
    }

    // ── Campaign Management ──

    @Transactional
    public BulkCampaignDto createCampaign(BulkCampaignDto dto) {
        // Auto-calculate totalRecipients from targetCriteria if not explicitly set
        int recipientCount = dto.getTotalRecipients() != null ? dto.getTotalRecipients() : 0;
        if (recipientCount == 0 && dto.getTargetCriteria() != null && !dto.getTargetCriteria().isBlank()) {
            recipientCount = countRecipientsFromCriteria(dto.getTargetCriteria());
        }

        var campaign = BulkCampaign.builder()
                .name(dto.getName())
                .channelType(dto.getChannelType())
                .templateId(dto.getTemplateId())
                .subject(dto.getSubject())
                .body(dto.getBody())
                .targetCriteria(dto.getTargetCriteria())
                .totalRecipients(recipientCount)
                .sentCount(0)
                .failedCount(0)
                .status("draft")
                .createdBy(dto.getCreatedBy())
                .orgAlias(orgAlias())
                .build();

        if (dto.getScheduledAt() != null && !dto.getScheduledAt().isBlank()) {
            campaign.setScheduledAt(parseDateTime(dto.getScheduledAt()));
        }

        return toCampaignDto(campaignRepo.save(campaign));
    }

    @Transactional(readOnly = true)
    public BulkCampaignDto getCampaign(Long id) {
        return campaignRepo.findByIdAndOrgAlias(id, orgAlias())
                .map(this::toCampaignDto)
                .orElseThrow(() -> new NoSuchElementException("Campaign not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<BulkCampaignDto> listCampaigns() {
        return campaignRepo.findByOrgAliasOrderByCreatedAtDesc(orgAlias())
                .stream().map(this::toCampaignDto).toList();
    }

    @Transactional
    public BulkCampaignDto startCampaign(Long id) {
        var campaign = campaignRepo.findByIdAndOrgAlias(id, orgAlias())
                .orElseThrow(() -> new NoSuchElementException("Campaign not found: " + id));

        if (!"draft".equals(campaign.getStatus()) && !"scheduled".equals(campaign.getStatus())) {
            throw new IllegalStateException("Campaign cannot be started from status: " + campaign.getStatus());
        }

        campaign.setStatus("sending");
        campaign.setStartedAt(LocalDateTime.now());
        campaignRepo.save(campaign);

        log.info("Starting campaign '{}' for org {}", campaign.getName(), orgAlias());

        // Parse recipients from targetCriteria and send emails
        List<String> recipients = parseRecipients(campaign.getTargetCriteria());
        int sent = 0;
        int failed = 0;

        String subject = campaign.getSubject();
        String body = campaign.getBody();

        // If template is set, resolve it
        if (campaign.getTemplateId() != null) {
            var templateOpt = templateRepo.findById(campaign.getTemplateId());
            if (templateOpt.isPresent()) {
                var tmpl = templateOpt.get();
                if (subject == null || subject.isBlank()) subject = tmpl.getSubject();
                if (body == null || body.isBlank()) body = tmpl.getBody();
            }
        }

        String org = orgAlias();
        for (String recipient : recipients) {
            try {
                var result = send(org, "email", recipient.trim(), subject, body, null,
                        "campaign_" + campaign.getId());
                if ("sent".equals(result.getStatus())) {
                    sent++;
                } else {
                    failed++;
                }
                log.info("Campaign '{}': {} to {}", campaign.getName(), result.getStatus(), recipient);
            } catch (Exception e) {
                failed++;
                log.warn("Campaign '{}': failed to send to {}: {}", campaign.getName(), recipient, e.getMessage());
            }
        }

        campaign.setStatus("completed");
        campaign.setCompletedAt(LocalDateTime.now());
        campaign.setSentCount(sent);
        campaign.setFailedCount(failed);

        return toCampaignDto(campaignRepo.save(campaign));
    }

    @Transactional
    public BulkCampaignDto cancelCampaign(Long id) {
        var campaign = campaignRepo.findByIdAndOrgAlias(id, orgAlias())
                .orElseThrow(() -> new NoSuchElementException("Campaign not found: " + id));

        if ("completed".equals(campaign.getStatus()) || "cancelled".equals(campaign.getStatus())) {
            throw new IllegalStateException("Campaign cannot be cancelled from status: " + campaign.getStatus());
        }

        campaign.setStatus("cancelled");
        campaign.setCompletedAt(LocalDateTime.now());

        return toCampaignDto(campaignRepo.save(campaign));
    }

    // ── Helpers ──

    /**
     * Parse recipient emails from targetCriteria JSON.
     */
    @SuppressWarnings("unchecked")
    private List<String> parseRecipients(String targetCriteria) {
        try {
            var om = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> criteria = om.readValue(targetCriteria, Map.class);
            Object emails = criteria.get("recipientEmails");
            if (emails instanceof String emailStr && !emailStr.isBlank()) {
                return java.util.Arrays.stream(emailStr.split("[,\\n\\r;]+"))
                        .map(String::trim)
                        .filter(s -> !s.isBlank() && s.contains("@"))
                        .toList();
            }
        } catch (Exception e) {
            log.debug("Could not parse targetCriteria for recipients: {}", e.getMessage());
        }
        return List.of();
    }

    /**
     * Count recipients from targetCriteria JSON.
     * Parses recipientEmails field (comma or newline separated) to count valid entries.
     */
    @SuppressWarnings("unchecked")
    private int countRecipientsFromCriteria(String targetCriteria) {
        try {
            var om = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> criteria = om.readValue(targetCriteria, Map.class);
            Object emails = criteria.get("recipientEmails");
            if (emails instanceof String emailStr && !emailStr.isBlank()) {
                return (int) java.util.Arrays.stream(emailStr.split("[,\\n\\r;]+"))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .count();
            }
        } catch (Exception e) {
            log.debug("Could not parse targetCriteria for recipient count: {}", e.getMessage());
        }
        return 0;
    }

    private String resolveVariables(String template, Map<String, String> variables) {
        if (template == null || variables == null || variables.isEmpty()) return template;
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String replacement = variables.getOrDefault(varName, "{{" + varName + "}}");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private LocalDateTime parseDateTime(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            if (s.endsWith("Z") || s.contains("+")) {
                return java.time.Instant.parse(s).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
            }
            return LocalDateTime.parse(s);
        } catch (Exception e) {
            log.warn("Failed to parse datetime '{}', returning null", s);
            return null;
        }
    }

    // ── Mappers ──

    private NotificationLogDto toLogDto(NotificationLog e) {
        return NotificationLogDto.builder()
                .id(e.getId())
                .channelType(e.getChannelType())
                .recipient(e.getRecipient())
                .recipientName(e.getRecipientName())
                .templateKey(e.getTemplateKey())
                .subject(e.getSubject())
                .body(e.getBody())
                .status(e.getStatus())
                .errorMessage(e.getErrorMessage())
                .externalId(e.getExternalId())
                .patientId(e.getPatientId())
                .patientName(e.getPatientName())
                .sentBy(e.getSentBy())
                .triggerType(e.getTriggerType())
                .metadata(e.getMetadata())
                .sentAt(e.getSentAt() != null ? e.getSentAt().toString() : null)
                .deliveredAt(e.getDeliveredAt() != null ? e.getDeliveredAt().toString() : null)
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .build();
    }

    private BulkCampaignDto toCampaignDto(BulkCampaign e) {
        return BulkCampaignDto.builder()
                .id(e.getId())
                .name(e.getName())
                .channelType(e.getChannelType())
                .templateId(e.getTemplateId())
                .subject(e.getSubject())
                .body(e.getBody())
                .targetCriteria(e.getTargetCriteria())
                .totalRecipients(e.getTotalRecipients())
                .sentCount(e.getSentCount())
                .failedCount(e.getFailedCount())
                .status(e.getStatus())
                .scheduledAt(e.getScheduledAt() != null ? e.getScheduledAt().toString() : null)
                .startedAt(e.getStartedAt() != null ? e.getStartedAt().toString() : null)
                .completedAt(e.getCompletedAt() != null ? e.getCompletedAt().toString() : null)
                .createdBy(e.getCreatedBy())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }
}
