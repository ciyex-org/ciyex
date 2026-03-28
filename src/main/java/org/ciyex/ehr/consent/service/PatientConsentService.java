package org.ciyex.ehr.consent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.consent.dto.PatientConsentDto;
import org.ciyex.ehr.consent.entity.PatientConsent;
import org.ciyex.ehr.consent.repository.PatientConsentRepository;
import org.ciyex.ehr.dto.integration.RequestContext;
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
public class PatientConsentService {

    private final PatientConsentRepository repo;

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    // ── CRUD ──

    @Transactional
    public PatientConsentDto create(PatientConsentDto dto) {
        var consent = PatientConsent.builder()
                .patientId(dto.getPatientId())
                .patientName(dto.getPatientName())
                .consentType(dto.getConsentType())
                .status(dto.getStatus() != null ? dto.getStatus() : "pending")
                .signedDate(parseDate(dto.getSignedDate(), null))
                .expiryDate(parseDate(dto.getExpiryDate(), null))
                .signedBy(dto.getSignedBy())
                .witnessName(dto.getWitnessName())
                .documentUrl(dto.getDocumentUrl())
                .version(dto.getVersion())
                .notes(dto.getNotes())
                .orgAlias(orgAlias())
                .build();
        consent = repo.save(consent);
        return toDto(consent);
    }

    @Transactional(readOnly = true)
    public PatientConsentDto getById(Long id) {
        return repo.findById(id)
                .filter(c -> c.getOrgAlias().equals(orgAlias()))
                .map(this::toDto)
                .orElseThrow(() -> new NoSuchElementException("Consent not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<PatientConsentDto> getByPatient(Long patientId) {
        return repo.findByOrgAliasAndPatientIdOrderByCreatedAtDesc(orgAlias(), patientId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Page<PatientConsentDto> getAll(Pageable pageable) {
        return repo.findByOrgAlias(orgAlias(), pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<PatientConsentDto> search(String query) {
        if (query == null || query.isBlank()) {
            return repo.findByOrgAlias(orgAlias(), Pageable.unpaged())
                    .stream().map(this::toDto).toList();
        }
        return repo.search(orgAlias(), query.trim())
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public PatientConsentDto update(Long id, PatientConsentDto dto) {
        var consent = repo.findById(id)
                .filter(c -> c.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Consent not found: " + id));

        if (dto.getPatientName() != null) consent.setPatientName(dto.getPatientName());
        if (dto.getConsentType() != null) consent.setConsentType(dto.getConsentType());
        if (dto.getStatus() != null) consent.setStatus(dto.getStatus());
        if (dto.getSignedDate() != null) consent.setSignedDate(parseDate(dto.getSignedDate(), null));
        if (dto.getExpiryDate() != null) consent.setExpiryDate(parseDate(dto.getExpiryDate(), null));
        if (dto.getSignedBy() != null) consent.setSignedBy(dto.getSignedBy());
        if (dto.getWitnessName() != null) consent.setWitnessName(dto.getWitnessName());
        if (dto.getDocumentUrl() != null) consent.setDocumentUrl(dto.getDocumentUrl());
        if (dto.getVersion() != null) consent.setVersion(dto.getVersion());
        if (dto.getNotes() != null) consent.setNotes(dto.getNotes());

        return toDto(repo.save(consent));
    }

    @Transactional
    public void delete(Long id) {
        var consent = repo.findById(id)
                .filter(c -> c.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Consent not found: " + id));
        repo.delete(consent);
    }

    // ── Actions ──

    @Transactional
    public PatientConsentDto sign(Long id, String signedBy, String witnessName) {
        var consent = repo.findById(id)
                .filter(c -> c.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Consent not found: " + id));
        consent.setStatus("signed");
        consent.setSignedDate(LocalDate.now());
        consent.setSignedBy(signedBy);
        if (witnessName != null) consent.setWitnessName(witnessName);
        return toDto(repo.save(consent));
    }

    @Transactional
    public PatientConsentDto revoke(Long id) {
        var consent = repo.findById(id)
                .filter(c -> c.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Consent not found: " + id));
        consent.setStatus("revoked");
        return toDto(repo.save(consent));
    }

    // ── Stats ──

    @Transactional(readOnly = true)
    public Map<String, Long> getStats() {
        String org = orgAlias();
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("pending", repo.countByOrgAliasAndStatus(org, "pending"));
        stats.put("signed", repo.countByOrgAliasAndStatus(org, "signed"));
        stats.put("expired", repo.countByOrgAliasAndStatus(org, "expired"));
        stats.put("revoked", repo.countByOrgAliasAndStatus(org, "revoked"));
        return stats;
    }

    // ── Date parsing ──

    private LocalDate parseDate(String s, LocalDate fallback) {
        if (s == null || s.isBlank()) return fallback;
        try {
            if (s.contains("T")) return Instant.parse(s).atZone(ZoneId.systemDefault()).toLocalDate();
            if (s.matches("\\d{2}-\\d{2}-\\d{4}")) return LocalDate.parse(s, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            return LocalDate.parse(s);
        } catch (Exception e) {
            log.warn("Failed to parse date '{}', using fallback", s);
            return fallback;
        }
    }

    // ── Mapping ──

    private PatientConsentDto toDto(PatientConsent e) {
        return PatientConsentDto.builder()
                .id(e.getId())
                .patientId(e.getPatientId())
                .patientName(e.getPatientName())
                .consentType(e.getConsentType())
                .status(e.getStatus())
                .signedDate(e.getSignedDate() != null ? e.getSignedDate().toString() : null)
                .expiryDate(e.getExpiryDate() != null ? e.getExpiryDate().toString() : null)
                .signedBy(e.getSignedBy())
                .witnessName(e.getWitnessName())
                .documentUrl(e.getDocumentUrl())
                .version(e.getVersion())
                .notes(e.getNotes())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }
}
