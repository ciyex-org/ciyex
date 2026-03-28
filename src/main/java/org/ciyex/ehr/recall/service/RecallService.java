package org.ciyex.ehr.recall.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.recall.dto.PatientRecallDto;
import org.ciyex.ehr.recall.dto.RecallKpiDto;
import org.ciyex.ehr.recall.dto.RecallOutreachLogDto;
import org.ciyex.ehr.recall.dto.RecallTypeDto;
import org.ciyex.ehr.recall.entity.PatientRecall;
import org.ciyex.ehr.recall.entity.RecallOutreachLog;
import org.ciyex.ehr.recall.entity.RecallType;
import org.ciyex.ehr.recall.repository.PatientRecallRepository;
import org.ciyex.ehr.recall.repository.RecallOutreachLogRepository;
import org.ciyex.ehr.recall.repository.RecallTypeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecallService {

    private final PatientRecallRepository recallRepo;
    private final RecallTypeRepository typeRepo;
    private final RecallOutreachLogRepository outreachRepo;

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    // ═══════════════════════════════════════════════════════
    // RECALL TYPES
    // ═══════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<RecallTypeDto> getRecallTypes() {
        String org = orgAlias();
        var types = typeRepo.findByOrgAliasAndActiveOrderByNameAsc(org, true);
        if (types.isEmpty()) {
            // Auto-seed from __SYSTEM__ templates on first access
            seedRecallTypesForOrg(org);
            types = typeRepo.findByOrgAliasAndActiveOrderByNameAsc(org, true);
        }
        return types.stream().map(this::toTypeDto).toList();
    }

    @Transactional(readOnly = true)
    public List<RecallTypeDto> getAllRecallTypes() {
        return typeRepo.findByOrgAliasOrderByNameAsc(orgAlias())
                .stream().map(this::toTypeDto).toList();
    }

    @Transactional
    public RecallTypeDto createRecallType(RecallTypeDto dto) {
        var type = RecallType.builder()
                .orgAlias(orgAlias())
                .name(dto.getName())
                .code(dto.getCode())
                .category(dto.getCategory() != null ? dto.getCategory() : "PREVENTIVE")
                .intervalMonths(dto.getIntervalMonths() != null ? dto.getIntervalMonths() : 12)
                .intervalDays(dto.getIntervalDays() != null ? dto.getIntervalDays() : 0)
                .leadTimeDays(dto.getLeadTimeDays() != null ? dto.getLeadTimeDays() : 30)
                .maxAttempts(dto.getMaxAttempts() != null ? dto.getMaxAttempts() : 4)
                .priority(dto.getPriority() != null ? dto.getPriority() : "NORMAL")
                .autoCreate(dto.getAutoCreate() != null ? dto.getAutoCreate() : false)
                .communicationSequence(dto.getCommunicationSequence() != null
                        ? dto.getCommunicationSequence()
                        : List.of("PORTAL", "SMS", "PHONE", "LETTER"))
                .escalationWaitDays(dto.getEscalationWaitDays() != null
                        ? dto.getEscalationWaitDays()
                        : List.of(7, 7, 14))
                .appointmentTypeCode(dto.getAppointmentTypeCode())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .createdBy(dto.getCreatedBy())
                .build();
        type = typeRepo.save(type);
        return toTypeDto(type);
    }

    @Transactional
    public RecallTypeDto updateRecallType(Long id, RecallTypeDto dto) {
        var type = typeRepo.findById(id)
                .filter(t -> t.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Recall type not found: " + id));

        if (dto.getName() != null) type.setName(dto.getName());
        if (dto.getCode() != null) type.setCode(dto.getCode());
        if (dto.getCategory() != null) type.setCategory(dto.getCategory());
        if (dto.getIntervalMonths() != null) type.setIntervalMonths(dto.getIntervalMonths());
        if (dto.getIntervalDays() != null) type.setIntervalDays(dto.getIntervalDays());
        if (dto.getLeadTimeDays() != null) type.setLeadTimeDays(dto.getLeadTimeDays());
        if (dto.getMaxAttempts() != null) type.setMaxAttempts(dto.getMaxAttempts());
        if (dto.getPriority() != null) type.setPriority(dto.getPriority());
        if (dto.getAutoCreate() != null) type.setAutoCreate(dto.getAutoCreate());
        if (dto.getCommunicationSequence() != null) type.setCommunicationSequence(dto.getCommunicationSequence());
        if (dto.getEscalationWaitDays() != null) type.setEscalationWaitDays(dto.getEscalationWaitDays());
        if (dto.getAppointmentTypeCode() != null) type.setAppointmentTypeCode(dto.getAppointmentTypeCode());
        if (dto.getActive() != null) type.setActive(dto.getActive());

        return toTypeDto(typeRepo.save(type));
    }

    @Transactional
    public void seedRecallTypesForOrg(String orgAlias) {
        var systemTypes = typeRepo.findByOrgAliasOrderByNameAsc("__SYSTEM__");
        if (systemTypes.isEmpty()) {
            log.warn("No __SYSTEM__ recall type templates found to seed for org {}", orgAlias);
            return;
        }
        for (var tmpl : systemTypes) {
            // Skip if org already has this code
            if (typeRepo.findByOrgAliasAndCode(orgAlias, tmpl.getCode()).isPresent()) continue;

            var copy = RecallType.builder()
                    .orgAlias(orgAlias)
                    .name(tmpl.getName())
                    .code(tmpl.getCode())
                    .category(tmpl.getCategory())
                    .intervalMonths(tmpl.getIntervalMonths())
                    .intervalDays(tmpl.getIntervalDays())
                    .leadTimeDays(tmpl.getLeadTimeDays())
                    .maxAttempts(tmpl.getMaxAttempts())
                    .priority(tmpl.getPriority())
                    .autoCreate(tmpl.getAutoCreate())
                    .communicationSequence(tmpl.getCommunicationSequence())
                    .escalationWaitDays(tmpl.getEscalationWaitDays())
                    .appointmentTypeCode(tmpl.getAppointmentTypeCode())
                    .active(true)
                    .build();
            typeRepo.save(copy);
        }
        log.info("Seeded {} recall types for org {}", systemTypes.size(), orgAlias);
    }

    // ═══════════════════════════════════════════════════════
    // PATIENT RECALLS (CRUD)
    // ═══════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Page<PatientRecallDto> getRecalls(String status, Long typeId, Long providerId,
                                              String dueDateFrom, String dueDateTo,
                                              String search, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "dueDate"));
        return recallRepo.findFiltered(
                orgAlias(),
                status,
                typeId,
                providerId,
                parseDate(dueDateFrom, null),
                parseDate(dueDateTo, null),
                search,
                pageable
        ).map(this::toRecallDto);
    }

    @Transactional(readOnly = true)
    public PatientRecallDto getRecallById(Long id) {
        var recall = recallRepo.findById(id)
                .filter(r -> r.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Recall not found: " + id));

        var dto = toRecallDto(recall);
        // Include outreach logs in detail view
        var logs = outreachRepo.findByRecallIdOrderByAttemptNumberDesc(id);
        dto.setOutreachLogs(logs.stream().map(this::toOutreachDto).toList());
        return dto;
    }

    @Transactional(readOnly = true)
    public List<PatientRecallDto> getRecallsByPatient(Long patientId) {
        return recallRepo.findByOrgAliasAndPatientIdOrderByDueDateDesc(orgAlias(), patientId)
                .stream().map(this::toRecallDto).toList();
    }

    @Transactional
    public PatientRecallDto createRecall(PatientRecallDto dto) {
        RecallType recallType = null;
        if (dto.getRecallTypeId() != null) {
            recallType = typeRepo.findById(dto.getRecallTypeId()).orElse(null);
        }

        var recall = PatientRecall.builder()
                .orgAlias(orgAlias())
                .patientId(dto.getPatientId())
                .patientName(dto.getPatientName())
                .patientPhone(dto.getPatientPhone())
                .patientEmail(dto.getPatientEmail())
                .recallType(recallType)
                .recallTypeName(dto.getRecallTypeName() != null ? dto.getRecallTypeName()
                        : (recallType != null ? recallType.getName() : null))
                .providerId(dto.getProviderId())
                .providerName(dto.getProviderName())
                .locationId(dto.getLocationId())
                .status(dto.getStatus() != null ? dto.getStatus() : "PENDING")
                .dueDate(parseDate(dto.getDueDate()))
                .notificationDate(parseDate(dto.getNotificationDate(), null))
                .sourceEncounterId(dto.getSourceEncounterId())
                .sourceAppointmentId(dto.getSourceAppointmentId())
                .linkedAppointmentId(dto.getLinkedAppointmentId())
                .preferredContact(dto.getPreferredContact() != null ? dto.getPreferredContact() : "PHONE")
                .priority(dto.getPriority() != null ? dto.getPriority()
                        : (recallType != null ? recallType.getPriority() : "NORMAL"))
                .notes(dto.getNotes())
                .autoCreated(dto.getAutoCreated() != null ? dto.getAutoCreated() : false)
                .attemptCount(0)
                .createdBy(dto.getCreatedBy())
                .updatedBy(dto.getUpdatedBy())
                .build();

        recall = recallRepo.save(recall);
        return toRecallDto(recall);
    }

    @Transactional
    public PatientRecallDto updateRecall(Long id, PatientRecallDto dto) {
        var recall = recallRepo.findById(id)
                .filter(r -> r.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Recall not found: " + id));

        if (dto.getPatientName() != null) recall.setPatientName(dto.getPatientName());
        if (dto.getPatientPhone() != null) recall.setPatientPhone(dto.getPatientPhone());
        if (dto.getPatientEmail() != null) recall.setPatientEmail(dto.getPatientEmail());
        if (dto.getRecallTypeId() != null) {
            var recallType = typeRepo.findById(dto.getRecallTypeId()).orElse(null);
            recall.setRecallType(recallType);
            if (recallType != null && dto.getRecallTypeName() == null) {
                recall.setRecallTypeName(recallType.getName());
            }
        }
        if (dto.getRecallTypeName() != null) recall.setRecallTypeName(dto.getRecallTypeName());
        if (dto.getProviderId() != null) recall.setProviderId(dto.getProviderId());
        if (dto.getProviderName() != null) recall.setProviderName(dto.getProviderName());
        if (dto.getLocationId() != null) recall.setLocationId(dto.getLocationId());
        if (dto.getStatus() != null) {
            recall.setStatus(dto.getStatus());
            if ("COMPLETED".equals(dto.getStatus()) && recall.getCompletedDate() == null) {
                recall.setCompletedDate(LocalDate.now());
            }
        }
        if (dto.getDueDate() != null) recall.setDueDate(parseDate(dto.getDueDate()));
        if (dto.getNotificationDate() != null) recall.setNotificationDate(parseDate(dto.getNotificationDate(), null));
        if (dto.getLinkedAppointmentId() != null) recall.setLinkedAppointmentId(dto.getLinkedAppointmentId());
        if (dto.getCompletedEncounterId() != null) recall.setCompletedEncounterId(dto.getCompletedEncounterId());
        if (dto.getCompletedDate() != null) recall.setCompletedDate(parseDate(dto.getCompletedDate(), null));
        if (dto.getPreferredContact() != null) recall.setPreferredContact(dto.getPreferredContact());
        if (dto.getPriority() != null) recall.setPriority(dto.getPriority());
        if (dto.getNotes() != null) recall.setNotes(dto.getNotes());
        if (dto.getCancelledReason() != null) recall.setCancelledReason(dto.getCancelledReason());
        if (dto.getUpdatedBy() != null) recall.setUpdatedBy(dto.getUpdatedBy());

        return toRecallDto(recallRepo.save(recall));
    }

    @Transactional
    public void deleteRecall(Long id) {
        var recall = recallRepo.findById(id)
                .filter(r -> r.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Recall not found: " + id));
        recallRepo.delete(recall);
    }

    // ═══════════════════════════════════════════════════════
    // KPIs
    // ═══════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public RecallKpiDto getKpis() {
        String org = orgAlias();
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

        long dueToday = recallRepo.countDueToday(org, today);
        long overdue = recallRepo.countOverdue(org, today);
        long completedThisMonth = recallRepo.countCompletedThisMonth(org, startOfMonth, endOfMonth);
        long pending = recallRepo.countByOrgAliasAndStatus(org, "PENDING");
        long contacted = recallRepo.countByOrgAliasAndStatus(org, "CONTACTED");
        long scheduled = recallRepo.countByOrgAliasAndStatus(org, "SCHEDULED");
        long cancelled = recallRepo.countByOrgAliasAndStatus(org, "CANCELLED");

        double complianceRate = 0.0;
        if (completedThisMonth + overdue > 0) {
            complianceRate = Math.round((double) completedThisMonth / (completedThisMonth + overdue) * 10000.0) / 100.0;
        }

        return RecallKpiDto.builder()
                .dueToday(dueToday)
                .overdue(overdue)
                .completedThisMonth(completedThisMonth)
                .pendingTotal(pending)
                .contactedTotal(contacted)
                .scheduledTotal(scheduled)
                .cancelledTotal(cancelled)
                .complianceRate(complianceRate)
                .build();
    }

    // ═══════════════════════════════════════════════════════
    // OUTREACH LOGGING
    // ═══════════════════════════════════════════════════════

    @Transactional
    public RecallOutreachLogDto logOutreach(Long recallId, RecallOutreachLogDto dto) {
        var recall = recallRepo.findById(recallId)
                .filter(r -> r.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Recall not found: " + recallId));

        int nextAttempt = recall.getAttemptCount() + 1;

        var logEntry = RecallOutreachLog.builder()
                .recall(recall)
                .orgAlias(orgAlias())
                .attemptNumber(nextAttempt)
                .attemptDate(dto.getAttemptDate() != null ? parseDateTime(dto.getAttemptDate()) : LocalDateTime.now())
                .method(dto.getMethod())
                .direction(dto.getDirection() != null ? dto.getDirection() : "OUTBOUND")
                .performedBy(dto.getPerformedBy())
                .performedByName(dto.getPerformedByName())
                .outcome(dto.getOutcome())
                .notes(dto.getNotes())
                .nextAction(dto.getNextAction())
                .nextActionDate(parseDate(dto.getNextActionDate(), null))
                .automated(dto.getAutomated() != null ? dto.getAutomated() : false)
                .deliveryStatus(dto.getDeliveryStatus())
                .build();
        logEntry = outreachRepo.save(logEntry);

        // Update recall with latest attempt info
        recall.setAttemptCount(nextAttempt);
        recall.setLastAttemptDate(logEntry.getAttemptDate());
        recall.setLastAttemptMethod(logEntry.getMethod());
        recall.setLastAttemptOutcome(logEntry.getOutcome());
        if (logEntry.getNextActionDate() != null) {
            recall.setNextAttemptDate(logEntry.getNextActionDate());
        }
        // Auto-update status based on outcome
        if ("SCHEDULED".equals(dto.getOutcome())) {
            recall.setStatus("SCHEDULED");
        } else if ("DECLINED".equals(dto.getOutcome())) {
            recall.setStatus("CANCELLED");
            recall.setCancelledReason("Patient declined");
        } else if (!"COMPLETED".equals(recall.getStatus()) && !"SCHEDULED".equals(recall.getStatus())) {
            recall.setStatus("CONTACTED");
        }
        recallRepo.save(recall);

        return toOutreachDto(logEntry);
    }

    // ═══════════════════════════════════════════════════════
    // DATE PARSING
    // ═══════════════════════════════════════════════════════

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return LocalDate.now();
        return parseDateInternal(s, LocalDate.now());
    }

    private LocalDate parseDate(String s, LocalDate fallback) {
        if (s == null || s.isBlank()) return fallback;
        return parseDateInternal(s, fallback);
    }

    private LocalDate parseDateInternal(String s, LocalDate fallback) {
        try {
            if (s.contains("T")) return Instant.parse(s).atZone(ZoneId.systemDefault()).toLocalDate();
            if (s.matches("\\d{2}-\\d{2}-\\d{4}")) return LocalDate.parse(s, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            return LocalDate.parse(s);
        } catch (Exception e) {
            log.warn("Failed to parse date '{}', using fallback", s);
            return fallback;
        }
    }

    private LocalDateTime parseDateTime(String s) {
        if (s == null || s.isBlank()) return LocalDateTime.now();
        try {
            if (s.endsWith("Z") || s.contains("+")) return Instant.parse(s).atZone(ZoneId.systemDefault()).toLocalDateTime();
            return LocalDateTime.parse(s);
        } catch (Exception e) {
            log.warn("Failed to parse datetime '{}', returning now", s);
            return LocalDateTime.now();
        }
    }

    // ═══════════════════════════════════════════════════════
    // DTO MAPPING
    // ═══════════════════════════════════════════════════════

    private RecallTypeDto toTypeDto(RecallType e) {
        return RecallTypeDto.builder()
                .id(e.getId())
                .name(e.getName())
                .code(e.getCode())
                .category(e.getCategory())
                .intervalMonths(e.getIntervalMonths())
                .intervalDays(e.getIntervalDays())
                .leadTimeDays(e.getLeadTimeDays())
                .maxAttempts(e.getMaxAttempts())
                .priority(e.getPriority())
                .autoCreate(e.getAutoCreate())
                .communicationSequence(e.getCommunicationSequence())
                .escalationWaitDays(e.getEscalationWaitDays())
                .appointmentTypeCode(e.getAppointmentTypeCode())
                .active(e.getActive())
                .createdBy(e.getCreatedBy())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }

    private PatientRecallDto toRecallDto(PatientRecall e) {
        return PatientRecallDto.builder()
                .id(e.getId())
                .patientId(e.getPatientId())
                .patientName(e.getPatientName())
                .patientPhone(e.getPatientPhone())
                .patientEmail(e.getPatientEmail())
                .recallTypeId(e.getRecallType() != null ? e.getRecallType().getId() : null)
                .recallTypeName(e.getRecallTypeName())
                .recallTypeCode(e.getRecallType() != null ? e.getRecallType().getCode() : null)
                .recallTypeCategory(e.getRecallType() != null ? e.getRecallType().getCategory() : null)
                .providerId(e.getProviderId())
                .providerName(e.getProviderName())
                .locationId(e.getLocationId())
                .status(e.getStatus())
                .dueDate(e.getDueDate() != null ? e.getDueDate().toString() : null)
                .notificationDate(e.getNotificationDate() != null ? e.getNotificationDate().toString() : null)
                .sourceEncounterId(e.getSourceEncounterId())
                .sourceAppointmentId(e.getSourceAppointmentId())
                .linkedAppointmentId(e.getLinkedAppointmentId())
                .completedEncounterId(e.getCompletedEncounterId())
                .completedDate(e.getCompletedDate() != null ? e.getCompletedDate().toString() : null)
                .attemptCount(e.getAttemptCount())
                .lastAttemptDate(e.getLastAttemptDate() != null ? e.getLastAttemptDate().toString() : null)
                .lastAttemptMethod(e.getLastAttemptMethod())
                .lastAttemptOutcome(e.getLastAttemptOutcome())
                .nextAttemptDate(e.getNextAttemptDate() != null ? e.getNextAttemptDate().toString() : null)
                .preferredContact(e.getPreferredContact())
                .priority(e.getPriority())
                .notes(e.getNotes())
                .cancelledReason(e.getCancelledReason())
                .autoCreated(e.getAutoCreated())
                .createdBy(e.getCreatedBy())
                .updatedBy(e.getUpdatedBy())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }

    private RecallOutreachLogDto toOutreachDto(RecallOutreachLog e) {
        return RecallOutreachLogDto.builder()
                .id(e.getId())
                .recallId(e.getRecall() != null ? e.getRecall().getId() : null)
                .attemptNumber(e.getAttemptNumber())
                .attemptDate(e.getAttemptDate() != null ? e.getAttemptDate().toString() : null)
                .method(e.getMethod())
                .direction(e.getDirection())
                .performedBy(e.getPerformedBy())
                .performedByName(e.getPerformedByName())
                .outcome(e.getOutcome())
                .notes(e.getNotes())
                .nextAction(e.getNextAction())
                .nextActionDate(e.getNextActionDate() != null ? e.getNextActionDate().toString() : null)
                .automated(e.getAutomated())
                .deliveryStatus(e.getDeliveryStatus())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .build();
    }
}
