package org.ciyex.ehr.priorauth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.priorauth.dto.PriorAuthDto;
import org.ciyex.ehr.priorauth.entity.PriorAuthorization;
import org.ciyex.ehr.priorauth.repository.PriorAuthRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriorAuthService {

    private final PriorAuthRepository repo;

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    // ── List (paginated) ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<PriorAuthDto> list(Pageable pageable) {
        return repo.findByOrgAliasOrderByCreatedAtDesc(orgAlias(), pageable)
                .map(this::toDto);
    }

    // ── Get by ID ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PriorAuthDto getById(Long id) {
        return repo.findById(id)
                .filter(p -> p.getOrgAlias().equals(orgAlias()))
                .map(this::toDto)
                .orElseThrow(() -> new NoSuchElementException("Prior auth not found: " + id));
    }

    // ── Get by patient ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PriorAuthDto> getByPatient(Long patientId) {
        return repo.findByOrgAliasAndPatientIdOrderByRequestedDateDesc(orgAlias(), patientId)
                .stream().map(this::toDto).toList();
    }

    // ── List by status ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<PriorAuthDto> listByStatus(String status, Pageable pageable) {
        return repo.findByOrgAliasAndStatusOrderByCreatedAtDesc(orgAlias(), status, pageable)
                .map(this::toDto);
    }

    // ── Search ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PriorAuthDto> search(String query, String status) {
        if (query == null || query.isBlank()) {
            return repo.findByOrgAliasOrderByCreatedAtDesc(orgAlias(), Pageable.unpaged())
                    .stream().map(this::toDto).toList();
        }
        var results = repo.search(orgAlias(), query.trim())
                .stream();
        if (status != null && !status.isBlank()) {
            results = results.filter(p -> status.equalsIgnoreCase(p.getStatus()));
        }
        return results.map(this::toDto).toList();
    }

    // ── Stats ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Long> stats() {
        String org = orgAlias();
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("total", repo.countByOrgAlias(org));
        for (String s : List.of("pending", "submitted", "approved", "denied", "appeal", "expired", "cancelled")) {
            stats.put(s, repo.countByOrgAliasAndStatus(org, s));
        }
        return stats;
    }

    // ── Create ────────────────────────────────────────────────────────

    @Transactional
    public PriorAuthDto create(PriorAuthDto dto) {
        var entity = PriorAuthorization.builder()
                .patientId(dto.getPatientId())
                .patientName(dto.getPatientName())
                .providerName(dto.getProviderName())
                .insuranceName(dto.getInsuranceName())
                .insuranceId(dto.getInsuranceId())
                .memberId(dto.getMemberId())
                .authNumber(dto.getAuthNumber())
                .procedureCode(dto.getProcedureCode())
                .procedureDescription(dto.getProcedureDescription())
                .diagnosisCode(dto.getDiagnosisCode())
                .diagnosisDescription(dto.getDiagnosisDescription())
                .status(dto.getStatus() != null ? dto.getStatus() : "pending")
                .priority(dto.getPriority() != null ? dto.getPriority() : "routine")
                .requestedDate(parseDate(dto.getRequestedDate()))
                .reviewDate(parseDateNullable(dto.getReviewDate()))
                .approvedDate(parseDateNullable(dto.getApprovedDate()))
                .deniedDate(parseDateNullable(dto.getDeniedDate()))
                .expiryDate(parseDateNullable(dto.getExpiryDate()))
                .approvedUnits(dto.getApprovedUnits())
                .usedUnits(dto.getUsedUnits() != null ? dto.getUsedUnits() : 0)
                .remainingUnits(dto.getRemainingUnits())
                .denialReason(dto.getDenialReason())
                .appealDeadline(parseDateNullable(dto.getAppealDeadline()))
                .notes(dto.getNotes())
                .orgAlias(orgAlias())
                .build();
        entity = repo.save(entity);
        return toDto(entity);
    }

    // ── Update ────────────────────────────────────────────────────────

    @Transactional
    public PriorAuthDto update(Long id, PriorAuthDto dto) {
        var entity = repo.findById(id)
                .filter(p -> p.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Prior auth not found: " + id));

        if (dto.getPatientId() != null) entity.setPatientId(dto.getPatientId());
        if (dto.getPatientName() != null) entity.setPatientName(dto.getPatientName());
        if (dto.getProviderName() != null) entity.setProviderName(dto.getProviderName());
        if (dto.getInsuranceName() != null) entity.setInsuranceName(dto.getInsuranceName());
        if (dto.getInsuranceId() != null) entity.setInsuranceId(dto.getInsuranceId());
        if (dto.getMemberId() != null) entity.setMemberId(dto.getMemberId());
        if (dto.getAuthNumber() != null) entity.setAuthNumber(dto.getAuthNumber());
        if (dto.getProcedureCode() != null) entity.setProcedureCode(dto.getProcedureCode());
        if (dto.getProcedureDescription() != null) entity.setProcedureDescription(dto.getProcedureDescription());
        if (dto.getDiagnosisCode() != null) entity.setDiagnosisCode(dto.getDiagnosisCode());
        if (dto.getDiagnosisDescription() != null) entity.setDiagnosisDescription(dto.getDiagnosisDescription());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        if (dto.getPriority() != null) entity.setPriority(dto.getPriority());
        if (dto.getRequestedDate() != null) entity.setRequestedDate(parseDate(dto.getRequestedDate()));
        if (dto.getReviewDate() != null) entity.setReviewDate(parseDateNullable(dto.getReviewDate()));
        if (dto.getApprovedDate() != null) entity.setApprovedDate(parseDateNullable(dto.getApprovedDate()));
        if (dto.getDeniedDate() != null) entity.setDeniedDate(parseDateNullable(dto.getDeniedDate()));
        if (dto.getExpiryDate() != null) entity.setExpiryDate(parseDateNullable(dto.getExpiryDate()));
        if (dto.getApprovedUnits() != null) entity.setApprovedUnits(dto.getApprovedUnits());
        if (dto.getUsedUnits() != null) entity.setUsedUnits(dto.getUsedUnits());
        if (dto.getRemainingUnits() != null) entity.setRemainingUnits(dto.getRemainingUnits());
        if (dto.getDenialReason() != null) entity.setDenialReason(dto.getDenialReason());
        if (dto.getAppealDeadline() != null) entity.setAppealDeadline(parseDateNullable(dto.getAppealDeadline()));
        if (dto.getNotes() != null) entity.setNotes(dto.getNotes());

        return toDto(repo.save(entity));
    }

    // ── Approve ───────────────────────────────────────────────────────

    @Transactional
    public PriorAuthDto approve(Long id, PriorAuthDto dto) {
        var entity = repo.findById(id)
                .filter(p -> p.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Prior auth not found: " + id));

        entity.setStatus("approved");
        entity.setApprovedDate(LocalDate.now());
        if (dto.getAuthNumber() != null) entity.setAuthNumber(dto.getAuthNumber());
        if (dto.getApprovedUnits() != null) {
            entity.setApprovedUnits(dto.getApprovedUnits());
            entity.setRemainingUnits(dto.getApprovedUnits() - (entity.getUsedUnits() != null ? entity.getUsedUnits() : 0));
        }
        if (dto.getExpiryDate() != null) entity.setExpiryDate(parseDateNullable(dto.getExpiryDate()));

        return toDto(repo.save(entity));
    }

    // ── Deny ──────────────────────────────────────────────────────────

    @Transactional
    public PriorAuthDto deny(Long id, PriorAuthDto dto) {
        var entity = repo.findById(id)
                .filter(p -> p.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Prior auth not found: " + id));

        entity.setStatus("denied");
        entity.setDeniedDate(LocalDate.now());
        if (dto.getDenialReason() != null) entity.setDenialReason(dto.getDenialReason());
        if (dto.getAppealDeadline() != null) entity.setAppealDeadline(parseDateNullable(dto.getAppealDeadline()));

        return toDto(repo.save(entity));
    }

    // ── Delete ────────────────────────────────────────────────────────

    @Transactional
    public void delete(Long id) {
        var entity = repo.findById(id)
                .filter(p -> p.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Prior auth not found: " + id));
        repo.delete(entity);
    }

    // ── Date parsing ──────────────────────────────────────────────────

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return LocalDate.now();
        try {
            if (s.contains("T")) return Instant.parse(s).atZone(ZoneId.systemDefault()).toLocalDate();
            if (s.matches("\\d{2}-\\d{2}-\\d{4}")) return LocalDate.parse(s, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            if (s.matches("\\d{2}/\\d{2}/\\d{4}")) return LocalDate.parse(s, DateTimeFormatter.ofPattern("MM/dd/yyyy"));
            return LocalDate.parse(s);
        } catch (Exception e) {
            log.warn("Failed to parse date '{}', using today", s);
            return LocalDate.now();
        }
    }

    private LocalDate parseDateNullable(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            if (s.contains("T")) return Instant.parse(s).atZone(ZoneId.systemDefault()).toLocalDate();
            if (s.matches("\\d{2}-\\d{2}-\\d{4}")) return LocalDate.parse(s, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            if (s.matches("\\d{2}/\\d{2}/\\d{4}")) return LocalDate.parse(s, DateTimeFormatter.ofPattern("MM/dd/yyyy"));
            return LocalDate.parse(s);
        } catch (Exception e) {
            log.warn("Failed to parse date '{}', returning null", s);
            return null;
        }
    }

    // ── Entity → DTO ──────────────────────────────────────────────────

    private PriorAuthDto toDto(PriorAuthorization e) {
        return PriorAuthDto.builder()
                .id(e.getId())
                .patientId(e.getPatientId())
                .patientName(e.getPatientName())
                .providerName(e.getProviderName())
                .insuranceName(e.getInsuranceName())
                .insuranceId(e.getInsuranceId())
                .memberId(e.getMemberId())
                .authNumber(e.getAuthNumber())
                .procedureCode(e.getProcedureCode())
                .procedureDescription(e.getProcedureDescription())
                .diagnosisCode(e.getDiagnosisCode())
                .diagnosisDescription(e.getDiagnosisDescription())
                .status(e.getStatus())
                .priority(e.getPriority())
                .requestedDate(e.getRequestedDate() != null ? e.getRequestedDate().toString() : null)
                .reviewDate(e.getReviewDate() != null ? e.getReviewDate().toString() : null)
                .approvedDate(e.getApprovedDate() != null ? e.getApprovedDate().toString() : null)
                .deniedDate(e.getDeniedDate() != null ? e.getDeniedDate().toString() : null)
                .expiryDate(e.getExpiryDate() != null ? e.getExpiryDate().toString() : null)
                .approvedUnits(e.getApprovedUnits())
                .usedUnits(e.getUsedUnits())
                .remainingUnits(e.getRemainingUnits())
                .denialReason(e.getDenialReason())
                .appealDeadline(e.getAppealDeadline() != null ? e.getAppealDeadline().toString() : null)
                .notes(e.getNotes())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }
}
