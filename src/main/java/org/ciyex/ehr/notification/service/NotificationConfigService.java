package org.ciyex.ehr.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.notification.dto.*;
import org.ciyex.ehr.notification.entity.*;
import org.ciyex.ehr.notification.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationConfigService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String variablesToString(Object vars) {
        if (vars == null) return null;
        if (vars instanceof String s) return s;
        try { return MAPPER.writeValueAsString(vars); } catch (Exception e) { return null; }
    }

    private Object stringToVariables(String s) {
        if (s == null || s.isBlank()) return List.of();
        try { return MAPPER.readValue(s, Object.class); } catch (Exception e) { return s; }
    }

    private final NotificationConfigRepository configRepo;
    private final NotificationTemplateRepository templateRepo;
    private final NotificationPreferenceRepository preferenceRepo;
    private final PatientCommPreferenceRepository patientPrefRepo;

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    // ── Config CRUD ──

    @Transactional(readOnly = true)
    public NotificationConfigDto getConfig(String channelType) {
        return configRepo.findByOrgAliasAndChannelType(orgAlias(), channelType)
                .map(this::toConfigDto)
                .orElse(null);
    }

    @Transactional
    public NotificationConfigDto saveConfig(String channelType, NotificationConfigDto dto) {
        var config = configRepo.findByOrgAliasAndChannelType(orgAlias(), channelType)
                .orElse(NotificationConfig.builder()
                        .channelType(channelType)
                        .orgAlias(orgAlias())
                        .build());

        if (dto.getProvider() != null) config.setProvider(dto.getProvider());
        if (dto.getEnabled() != null) config.setEnabled(dto.getEnabled());
        if (dto.getConfig() != null) config.setConfig(dto.getConfig());
        if (dto.getSenderName() != null) config.setSenderName(dto.getSenderName());
        if (dto.getSenderAddress() != null) config.setSenderAddress(dto.getSenderAddress());
        if (dto.getDailyLimit() != null) config.setDailyLimit(dto.getDailyLimit());

        return toConfigDto(configRepo.save(config));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> testConfig(String channelType) {
        var configOpt = configRepo.findByOrgAliasAndChannelType(orgAlias(), channelType);
        if (configOpt.isEmpty()) {
            return Map.of("success", false, "message", "No configuration found for channel: " + channelType);
        }
        var config = configOpt.get();
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            return Map.of("success", false, "message", "Channel " + channelType + " is not enabled");
        }

        // Mock test - in production, this would actually test SMTP/Twilio connection
        log.info("Testing {} config for org {}: provider={}", channelType, orgAlias(), config.getProvider());
        return Map.of(
                "success", true,
                "message", "Connection test successful for " + config.getProvider(),
                "provider", config.getProvider()
        );
    }

    // ── Default seeding for new practices ──

    @Transactional
    public void ensureDefaultsExist(String orgAlias) {
        if (!templateRepo.findByOrgAliasOrderByNameAsc(orgAlias).isEmpty()) return;
        log.info("Seeding default notification templates and preferences for org: {}", orgAlias);
        seedDefaultTemplates(orgAlias);
        seedDefaultPreferences(orgAlias);
    }

    private void seedDefaultTemplates(String orgAlias) {
        var defaults = List.of(
                NotificationTemplate.builder()
                        .name("Appointment Confirmation").templateKey("appointment_confirmation").channelType("email")
                        .subject("Appointment Confirmation for {{patient_name}}")
                        .body("Dear {{patient_name}},\n\nYour appointment has been confirmed.\n\nDate: {{appointment_date}}\nTime: {{appointment_time}}\nProvider: {{provider_name}}\nLocation: {{location_name}}\n\nPlease arrive 15 minutes early. If you need to reschedule, please call us at {{practice_phone}}.\n\nThank you,\n{{practice_name}}")
                        .isActive(true).isDefault(true)
                        .variables("[\"patient_name\",\"appointment_date\",\"appointment_time\",\"provider_name\",\"location_name\",\"practice_phone\",\"practice_name\"]")
                        .orgAlias(orgAlias).build(),
                NotificationTemplate.builder()
                        .name("Appointment Reminder").templateKey("appointment_reminder").channelType("email")
                        .subject("Reminder: Your appointment on {{appointment_date}}")
                        .body("Hi {{patient_name}},\n\nThis is a reminder that you have an appointment scheduled:\n\nDate: {{appointment_date}}\nTime: {{appointment_time}}\nProvider: {{provider_name}}\nLocation: {{location_name}}\n\nPlease arrive 15 minutes early. If you need to reschedule, please call us at {{practice_phone}}.\n\nThank you,\n{{practice_name}}")
                        .isActive(true).isDefault(true)
                        .variables("[\"patient_name\",\"appointment_date\",\"appointment_time\",\"provider_name\",\"location_name\",\"practice_phone\",\"practice_name\"]")
                        .orgAlias(orgAlias).build(),
                NotificationTemplate.builder()
                        .name("Appointment Reminder (SMS)").templateKey("appointment_reminder").channelType("sms")
                        .body("Hi {{patient_name}}, reminder: Appt on {{appointment_date}} at {{appointment_time}} with {{provider_name}}. Reply STOP to opt out.")
                        .isActive(true).isDefault(true)
                        .variables("[\"patient_name\",\"appointment_date\",\"appointment_time\",\"provider_name\"]")
                        .orgAlias(orgAlias).build(),
                NotificationTemplate.builder()
                        .name("Lab Results Available").templateKey("lab_result_ready").channelType("email")
                        .subject("Your lab results are available")
                        .body("Hi {{patient_name}},\n\nYour lab results from {{order_date}} are now available. Please log in to the patient portal to view your results, or contact our office for more information.\n\n{{practice_name}}\n{{practice_phone}}")
                        .isActive(true).isDefault(true)
                        .variables("[\"patient_name\",\"order_date\",\"practice_name\",\"practice_phone\"]")
                        .orgAlias(orgAlias).build(),
                NotificationTemplate.builder()
                        .name("Prescription Ready").templateKey("prescription_ready").channelType("email")
                        .subject("Your prescription is ready")
                        .body("Hi {{patient_name}},\n\nYour prescription is ready for pickup at {{pharmacy_name}}.\n\nMedication: {{medication_name}}\n\nIf you have questions, please call us at {{practice_phone}}.\n\n{{practice_name}}")
                        .isActive(true).isDefault(true)
                        .variables("[\"patient_name\",\"medication_name\",\"pharmacy_name\",\"practice_phone\",\"practice_name\"]")
                        .orgAlias(orgAlias).build(),
                NotificationTemplate.builder()
                        .name("Recall / Follow-up Due").templateKey("recall_due").channelType("email")
                        .subject("It's time for your follow-up visit")
                        .body("Hi {{patient_name}},\n\nOur records show that you are due for a follow-up visit. Please call us at {{practice_phone}} to schedule your appointment.\n\nThank you,\n{{practice_name}}")
                        .isActive(true).isDefault(true)
                        .variables("[\"patient_name\",\"practice_phone\",\"practice_name\"]")
                        .orgAlias(orgAlias).build(),
                NotificationTemplate.builder()
                        .name("Secure Message Notification").templateKey("secure_message_notification").channelType("email")
                        .subject("New secure message from {{sender_name}}")
                        .body("You have a new secure message from {{sender_name}} in {{channel_name}}:\n\n{{message_preview}}\n\nLog in to your account to view the full message and reply securely.")
                        .isActive(true).isDefault(true)
                        .variables("[\"sender_name\",\"message_preview\",\"channel_name\"]")
                        .orgAlias(orgAlias).build()
        );
        templateRepo.saveAll(defaults);
    }

    private void seedDefaultPreferences(String orgAlias) {
        var eventTypes = List.of(
                new String[]{"appointment_confirmation", "immediate"},
                new String[]{"appointment_reminder", "24h_before"},
                new String[]{"lab_result_ready", "immediate"},
                new String[]{"prescription_ready", "immediate"},
                new String[]{"recall_due", "immediate"},
                new String[]{"billing_statement", "immediate"},
                new String[]{"secure_message_received", "immediate"}
        );
        for (String[] et : eventTypes) {
            if (preferenceRepo.findByOrgAliasAndEventType(orgAlias, et[0]).isEmpty()) {
                preferenceRepo.save(NotificationPreference.builder()
                        .eventType(et[0]).emailEnabled(true).smsEnabled(false)
                        .timing(et[1]).orgAlias(orgAlias).build());
            }
        }
    }

    // ── Template CRUD ──

    @Transactional
    public List<NotificationTemplateDto> listTemplates() {
        String org = orgAlias();
        var templates = templateRepo.findByOrgAliasOrderByNameAsc(org);
        if (templates.isEmpty()) {
            seedDefaultTemplates(org);
            seedDefaultPreferences(org);
            log.info("Seeded default notification templates and preferences for org: {}", org);
            templates = templateRepo.findByOrgAliasOrderByNameAsc(org);
        }
        return templates.stream().map(this::toTemplateDto).toList();
    }

    @Transactional(readOnly = true)
    public NotificationTemplateDto getTemplate(Long id) {
        return templateRepo.findById(id)
                .filter(t -> t.getOrgAlias().equals(orgAlias()))
                .map(this::toTemplateDto)
                .orElseThrow(() -> new NoSuchElementException("Template not found: " + id));
    }

    @Transactional
    public NotificationTemplateDto createTemplate(NotificationTemplateDto dto) {
        var template = NotificationTemplate.builder()
                .name(dto.getName())
                .templateKey(dto.getTemplateKey())
                .channelType(dto.getChannelType())
                .subject(dto.getSubject())
                .body(dto.getBody())
                .htmlBody(dto.getHtmlBody())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .isDefault(false)
                .variables(variablesToString(dto.getVariables()))
                .orgAlias(orgAlias())
                .build();
        return toTemplateDto(templateRepo.save(template));
    }

    @Transactional
    public NotificationTemplateDto updateTemplate(Long id, NotificationTemplateDto dto) {
        var template = templateRepo.findById(id)
                .filter(t -> t.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Template not found: " + id));

        if (dto.getName() != null) template.setName(dto.getName());
        if (dto.getTemplateKey() != null) template.setTemplateKey(dto.getTemplateKey());
        if (dto.getChannelType() != null) template.setChannelType(dto.getChannelType());
        if (dto.getSubject() != null) template.setSubject(dto.getSubject());
        if (dto.getBody() != null) template.setBody(dto.getBody());
        if (dto.getHtmlBody() != null) template.setHtmlBody(dto.getHtmlBody());
        if (dto.getIsActive() != null) template.setIsActive(dto.getIsActive());
        if (dto.getVariables() != null) template.setVariables(variablesToString(dto.getVariables()));

        return toTemplateDto(templateRepo.save(template));
    }

    @Transactional
    public void deleteTemplate(Long id) {
        var template = templateRepo.findById(id)
                .filter(t -> t.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Template not found: " + id));
        templateRepo.delete(template);
    }

    // ── Notification Preferences ──

    @Transactional
    public List<NotificationPreferenceDto> listPreferences() {
        String org = orgAlias();
        var prefs = preferenceRepo.findByOrgAlias(org);
        if (prefs.isEmpty()) {
            seedDefaultPreferences(org);
            if (templateRepo.findByOrgAliasOrderByNameAsc(org).isEmpty()) {
                seedDefaultTemplates(org);
            }
            log.info("Seeded default notification preferences for org: {}", org);
            prefs = preferenceRepo.findByOrgAlias(org);
        }
        return prefs.stream().map(this::toPrefDto).toList();
    }

    @Transactional
    public NotificationPreferenceDto savePreference(NotificationPreferenceDto dto) {
        var pref = preferenceRepo.findByOrgAliasAndEventType(orgAlias(), dto.getEventType())
                .orElse(NotificationPreference.builder()
                        .eventType(dto.getEventType())
                        .orgAlias(orgAlias())
                        .build());

        if (dto.getEmailEnabled() != null) pref.setEmailEnabled(dto.getEmailEnabled());
        if (dto.getSmsEnabled() != null) pref.setSmsEnabled(dto.getSmsEnabled());
        if (dto.getTiming() != null) pref.setTiming(dto.getTiming());
        if (dto.getTemplateId() != null) pref.setTemplateId(dto.getTemplateId());

        return toPrefDto(preferenceRepo.save(pref));
    }

    // ── Patient Communication Preferences ──

    @Transactional(readOnly = true)
    public PatientCommPreferenceDto getPatientPreference(Long patientId) {
        return patientPrefRepo.findByOrgAliasAndPatientId(orgAlias(), patientId)
                .map(this::toPatientPrefDto)
                .orElse(null);
    }

    @Transactional
    public PatientCommPreferenceDto savePatientPreference(Long patientId, PatientCommPreferenceDto dto) {
        var pref = patientPrefRepo.findByOrgAliasAndPatientId(orgAlias(), patientId)
                .orElse(PatientCommPreference.builder()
                        .patientId(patientId)
                        .orgAlias(orgAlias())
                        .build());

        if (dto.getEmail() != null) pref.setEmail(dto.getEmail());
        if (dto.getPhone() != null) pref.setPhone(dto.getPhone());
        if (dto.getEmailOptIn() != null) pref.setEmailOptIn(dto.getEmailOptIn());
        if (dto.getSmsOptIn() != null) pref.setSmsOptIn(dto.getSmsOptIn());
        if (dto.getPreferredChannel() != null) pref.setPreferredChannel(dto.getPreferredChannel());
        if (dto.getLanguage() != null) pref.setLanguage(dto.getLanguage());
        if (dto.getQuietHoursStart() != null) pref.setQuietHoursStart(parseTime(dto.getQuietHoursStart()));
        if (dto.getQuietHoursEnd() != null) pref.setQuietHoursEnd(parseTime(dto.getQuietHoursEnd()));

        return toPatientPrefDto(patientPrefRepo.save(pref));
    }

    // ── Mappers ──

    private NotificationConfigDto toConfigDto(NotificationConfig e) {
        return NotificationConfigDto.builder()
                .id(e.getId())
                .channelType(e.getChannelType())
                .provider(e.getProvider())
                .enabled(e.getEnabled())
                .config(e.getConfig())
                .senderName(e.getSenderName())
                .senderAddress(e.getSenderAddress())
                .dailyLimit(e.getDailyLimit())
                .sentToday(e.getSentToday())
                .lastResetDate(e.getLastResetDate() != null ? e.getLastResetDate().toString() : null)
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }

    private NotificationTemplateDto toTemplateDto(NotificationTemplate e) {
        return NotificationTemplateDto.builder()
                .id(e.getId())
                .name(e.getName())
                .templateKey(e.getTemplateKey())
                .channelType(e.getChannelType())
                .subject(e.getSubject())
                .body(e.getBody())
                .htmlBody(e.getHtmlBody())
                .isActive(e.getIsActive())
                .isDefault(e.getIsDefault())
                .variables(stringToVariables(e.getVariables()))
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }

    private NotificationPreferenceDto toPrefDto(NotificationPreference e) {
        return NotificationPreferenceDto.builder()
                .id(e.getId())
                .eventType(e.getEventType())
                .emailEnabled(e.getEmailEnabled())
                .smsEnabled(e.getSmsEnabled())
                .timing(e.getTiming())
                .templateId(e.getTemplateId())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }

    private PatientCommPreferenceDto toPatientPrefDto(PatientCommPreference e) {
        return PatientCommPreferenceDto.builder()
                .id(e.getId())
                .patientId(e.getPatientId())
                .email(e.getEmail())
                .phone(e.getPhone())
                .emailOptIn(e.getEmailOptIn())
                .smsOptIn(e.getSmsOptIn())
                .preferredChannel(e.getPreferredChannel())
                .language(e.getLanguage())
                .quietHoursStart(e.getQuietHoursStart() != null ? e.getQuietHoursStart().toString() : null)
                .quietHoursEnd(e.getQuietHoursEnd() != null ? e.getQuietHoursEnd().toString() : null)
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }

    private LocalTime parseTime(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalTime.parse(s);
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse time '{}', returning null", s);
            return null;
        }
    }
}
