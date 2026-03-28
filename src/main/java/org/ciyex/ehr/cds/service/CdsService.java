package org.ciyex.ehr.cds.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.cds.dto.CdsAlertLogDto;
import org.ciyex.ehr.cds.dto.CdsRuleDto;
import org.ciyex.ehr.cds.entity.CdsAlertLog;
import org.ciyex.ehr.cds.entity.CdsRule;
import org.ciyex.ehr.cds.repository.CdsAlertLogRepository;
import org.ciyex.ehr.cds.repository.CdsRuleRepository;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CdsService {

    private final CdsRuleRepository ruleRepo;
    private final CdsAlertLogRepository alertRepo;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Convert Object (Map/List/String) from DTO to JSON string for DB storage. */
    private String conditionsToString(Object obj) {
        if (obj == null) return "{}";
        if (obj instanceof String s) return s.isBlank() ? "{}" : s;
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    /** Convert JSON string from DB to Object for DTO response. */
    private Object stringToConditions(String str) {
        if (str == null || str.isBlank()) return java.util.Map.of();
        try {
            return MAPPER.readValue(str, Object.class);
        } catch (Exception e) {
            return java.util.Map.of();
        }
    }

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    // ─── Rules CRUD ───

    @Transactional
    public CdsRuleDto createRule(CdsRuleDto dto) {
        var rule = CdsRule.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .ruleType(dto.getRuleType())
                .category(dto.getCategory())
                .triggerEvent(dto.getTriggerEvent())
                .conditions(conditionsToString(dto.getConditions()))
                .actionType(dto.getActionType() != null ? dto.getActionType() : "alert")
                .severity(dto.getSeverity() != null ? dto.getSeverity() : "info")
                .message(dto.getMessage())
                .recommendation(dto.getRecommendation())
                .referenceUrl(dto.getReferenceUrl())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .appliesTo(dto.getAppliesTo() != null ? dto.getAppliesTo() : "all")
                .snoozeDays(dto.getSnoozeDays() != null ? dto.getSnoozeDays() : 0)
                .orgAlias(orgAlias())
                .build();
        return toRuleDto(ruleRepo.save(rule));
    }

    @Transactional(readOnly = true)
    public CdsRuleDto getRuleById(Long id) {
        return ruleRepo.findById(id)
                .filter(r -> r.getOrgAlias().equals(orgAlias()))
                .map(this::toRuleDto)
                .orElseThrow(() -> new NoSuchElementException("CDS rule not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<CdsRuleDto> getAllRules(Pageable pageable) {
        return ruleRepo.findByOrgAlias(orgAlias(), pageable).map(this::toRuleDto);
    }

    @Transactional(readOnly = true)
    public List<CdsRuleDto> searchRules(String query) {
        if (query == null || query.isBlank()) {
            return ruleRepo.findByOrgAlias(orgAlias(), Pageable.unpaged())
                    .map(this::toRuleDto).getContent();
        }
        return ruleRepo.search(orgAlias(), query.trim())
                .stream().map(this::toRuleDto).toList();
    }

    @Transactional
    public CdsRuleDto updateRule(Long id, CdsRuleDto dto) {
        var rule = ruleRepo.findById(id)
                .filter(r -> r.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("CDS rule not found: " + id));

        if (dto.getName() != null) rule.setName(dto.getName());
        if (dto.getDescription() != null) rule.setDescription(dto.getDescription());
        if (dto.getRuleType() != null) rule.setRuleType(dto.getRuleType());
        if (dto.getCategory() != null) rule.setCategory(dto.getCategory());
        if (dto.getTriggerEvent() != null) rule.setTriggerEvent(dto.getTriggerEvent());
        if (dto.getConditions() != null) rule.setConditions(conditionsToString(dto.getConditions()));
        if (dto.getActionType() != null) rule.setActionType(dto.getActionType());
        if (dto.getSeverity() != null) rule.setSeverity(dto.getSeverity());
        if (dto.getMessage() != null) rule.setMessage(dto.getMessage());
        if (dto.getRecommendation() != null) rule.setRecommendation(dto.getRecommendation());
        if (dto.getReferenceUrl() != null) rule.setReferenceUrl(dto.getReferenceUrl());
        if (dto.getAppliesTo() != null) rule.setAppliesTo(dto.getAppliesTo());
        if (dto.getSnoozeDays() != null) rule.setSnoozeDays(dto.getSnoozeDays());
        if (dto.getIsActive() != null) rule.setIsActive(dto.getIsActive());

        return toRuleDto(ruleRepo.save(rule));
    }

    @Transactional
    public void deleteRule(Long id) {
        var rule = ruleRepo.findById(id)
                .filter(r -> r.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("CDS rule not found: " + id));
        ruleRepo.delete(rule);
    }

    @Transactional
    public CdsRuleDto toggleActive(Long id) {
        var rule = ruleRepo.findById(id)
                .filter(r -> r.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("CDS rule not found: " + id));
        rule.setIsActive(!Boolean.TRUE.equals(rule.getIsActive()));
        return toRuleDto(ruleRepo.save(rule));
    }

    // ─── Evaluation (simplified) ───

    @Transactional(readOnly = true)
    public List<CdsRuleDto> evaluateForPatient(Long patientId, String triggerEvent) {
        return ruleRepo.findByOrgAliasAndIsActiveTrueAndTriggerEvent(orgAlias(), triggerEvent)
                .stream().map(this::toRuleDto).toList();
    }

    // ─── Alert Log ───

    @Transactional
    public CdsAlertLogDto logAlert(Long ruleId, Long patientId, CdsAlertLogDto dto) {
        var alert = CdsAlertLog.builder()
                .ruleId(ruleId)
                .ruleName(dto.getRuleName())
                .patientId(patientId)
                .patientName(dto.getPatientName())
                .encounterId(dto.getEncounterId())
                .alertType(dto.getAlertType())
                .severity(dto.getSeverity())
                .message(dto.getMessage())
                .orgAlias(orgAlias())
                .build();
        return toAlertDto(alertRepo.save(alert));
    }

    @Transactional
    public CdsAlertLogDto acknowledgeAlert(Long alertId, String action, String reason, String actedBy) {
        var alert = alertRepo.findById(alertId)
                .filter(a -> a.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("CDS alert not found: " + alertId));
        alert.setActionTaken(action);
        alert.setOverrideReason(reason);
        alert.setActedBy(actedBy);
        alert.setActedAt(LocalDateTime.now());
        return toAlertDto(alertRepo.save(alert));
    }

    @Transactional(readOnly = true)
    public Page<CdsAlertLogDto> getAllAlerts(Pageable pageable) {
        return alertRepo.findByOrgAlias(orgAlias(), pageable).map(this::toAlertDto);
    }

    @Transactional(readOnly = true)
    public List<CdsAlertLogDto> getAlertsByPatient(Long patientId) {
        return alertRepo.findByOrgAliasAndPatientId(orgAlias(), patientId)
                .stream().map(this::toAlertDto).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> alertStats() {
        String org = orgAlias();
        return Map.of(
                "total", alertRepo.countByOrgAlias(org),
                "acknowledged", alertRepo.countByOrgAliasAndActionTaken(org, "acknowledged"),
                "overridden", alertRepo.countByOrgAliasAndActionTaken(org, "overridden"),
                "acted_on", alertRepo.countByOrgAliasAndActionTaken(org, "acted_on"),
                "snoozed", alertRepo.countByOrgAliasAndActionTaken(org, "snoozed"),
                "dismissed", alertRepo.countByOrgAliasAndActionTaken(org, "dismissed")
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Long> cdsStats() {
        String org = orgAlias();
        long totalRules = ruleRepo.countByOrgAlias(org);
        long activeRules = ruleRepo.countByOrgAliasAndIsActiveTrue(org);
        long totalAlerts = alertRepo.countByOrgAlias(org);
        long alertsToday = alertRepo.countByOrgAliasAndCreatedAtAfter(org,
                java.time.LocalDateTime.now().toLocalDate().atStartOfDay());
        long alerts7d = alertRepo.countByOrgAliasAndCreatedAtAfter(org,
                java.time.LocalDateTime.now().minusDays(7));
        long criticalAlerts = alertRepo.countByOrgAliasAndSeverity(org, "critical");
        long overridden = alertRepo.countByOrgAliasAndActionTaken(org, "overridden");
        long overrideRate = totalAlerts > 0 ? (overridden * 100 / totalAlerts) : 0;
        return Map.of(
                "totalRules", totalRules,
                "activeRules", activeRules,
                "alertsToday", alertsToday,
                "alerts7d", alerts7d,
                "criticalAlerts", criticalAlerts,
                "overrideRate", overrideRate
        );
    }

    // ─── Mappers ───

    private CdsRuleDto toRuleDto(CdsRule e) {
        return CdsRuleDto.builder()
                .id(e.getId())
                .name(e.getName())
                .description(e.getDescription())
                .ruleType(e.getRuleType())
                .category(e.getCategory())
                .triggerEvent(e.getTriggerEvent())
                .conditions(stringToConditions(e.getConditions()))
                .actionType(e.getActionType())
                .severity(e.getSeverity())
                .message(e.getMessage())
                .recommendation(e.getRecommendation())
                .referenceUrl(e.getReferenceUrl())
                .isActive(e.getIsActive())
                .appliesTo(e.getAppliesTo())
                .snoozeDays(e.getSnoozeDays())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }

    private CdsAlertLogDto toAlertDto(CdsAlertLog e) {
        return CdsAlertLogDto.builder()
                .id(e.getId())
                .ruleId(e.getRuleId())
                .ruleName(e.getRuleName())
                .patientId(e.getPatientId())
                .patientName(e.getPatientName())
                .encounterId(e.getEncounterId())
                .alertType(e.getAlertType())
                .severity(e.getSeverity())
                .message(e.getMessage())
                .actionTaken(e.getActionTaken())
                .overrideReason(e.getOverrideReason())
                .actedBy(e.getActedBy())
                .actedAt(e.getActedAt() != null ? e.getActedAt().toString() : null)
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .build();
    }
}
