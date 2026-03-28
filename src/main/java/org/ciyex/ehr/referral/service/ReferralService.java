package org.ciyex.ehr.referral.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.referral.dto.ReferralDto;
import org.ciyex.ehr.referral.entity.Referral;
import org.ciyex.ehr.referral.repository.ReferralRepository;
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
public class ReferralService {

    private final ReferralRepository repo;

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    // ── CRUD ──

    @Transactional
    public ReferralDto create(ReferralDto dto) {
        var referral = Referral.builder()
                .patientId(dto.getPatientId())
                .patientName(dto.getPatientName())
                .referringProvider(dto.getReferringProvider())
                .specialistName(dto.getSpecialistName())
                .specialistNpi(dto.getSpecialistNpi())
                .specialty(dto.getSpecialty())
                .facilityName(dto.getFacilityName())
                .facilityAddress(dto.getFacilityAddress())
                .facilityPhone(dto.getFacilityPhone())
                .facilityFax(dto.getFacilityFax())
                .reason(dto.getReason())
                .clinicalNotes(dto.getClinicalNotes())
                .urgency(dto.getUrgency() != null ? dto.getUrgency() : "routine")
                .status(dto.getStatus() != null ? dto.getStatus() : "draft")
                .referralDate(parseDate(dto.getReferralDate()))
                .expiryDate(parseDate(dto.getExpiryDate(), null))
                .authorizationNumber(dto.getAuthorizationNumber())
                .insuranceName(dto.getInsuranceName())
                .insuranceId(dto.getInsuranceId())
                .appointmentDate(parseDate(dto.getAppointmentDate(), null))
                .appointmentNotes(dto.getAppointmentNotes())
                .followUpNotes(dto.getFollowUpNotes())
                .orgAlias(orgAlias())
                .build();
        referral = repo.save(referral);
        return toDto(referral);
    }

    @Transactional(readOnly = true)
    public ReferralDto getById(Long id) {
        return repo.findById(id)
                .filter(r -> r.getOrgAlias().equals(orgAlias()))
                .map(this::toDto)
                .orElseThrow(() -> new NoSuchElementException("Referral not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<ReferralDto> getByPatient(Long patientId) {
        return repo.findByOrgAliasAndPatientIdOrderByReferralDateDesc(orgAlias(), patientId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Page<ReferralDto> getAll(Pageable pageable) {
        return repo.findByOrgAlias(orgAlias(), pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<ReferralDto> search(String query) {
        if (query == null || query.isBlank()) {
            return repo.findByOrgAlias(orgAlias(), Pageable.unpaged())
                    .stream().map(this::toDto).toList();
        }
        return repo.search(orgAlias(), query.trim())
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public ReferralDto update(Long id, ReferralDto dto) {
        var referral = repo.findById(id)
                .filter(r -> r.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Referral not found: " + id));

        if (dto.getPatientName() != null) referral.setPatientName(dto.getPatientName());
        if (dto.getReferringProvider() != null) referral.setReferringProvider(dto.getReferringProvider());
        if (dto.getSpecialistName() != null) referral.setSpecialistName(dto.getSpecialistName());
        if (dto.getSpecialistNpi() != null) referral.setSpecialistNpi(dto.getSpecialistNpi());
        if (dto.getSpecialty() != null) referral.setSpecialty(dto.getSpecialty());
        if (dto.getFacilityName() != null) referral.setFacilityName(dto.getFacilityName());
        if (dto.getFacilityAddress() != null) referral.setFacilityAddress(dto.getFacilityAddress());
        if (dto.getFacilityPhone() != null) referral.setFacilityPhone(dto.getFacilityPhone());
        if (dto.getFacilityFax() != null) referral.setFacilityFax(dto.getFacilityFax());
        if (dto.getReason() != null) referral.setReason(dto.getReason());
        if (dto.getClinicalNotes() != null) referral.setClinicalNotes(dto.getClinicalNotes());
        if (dto.getUrgency() != null) referral.setUrgency(dto.getUrgency());
        if (dto.getStatus() != null) referral.setStatus(dto.getStatus());
        if (dto.getReferralDate() != null) referral.setReferralDate(parseDate(dto.getReferralDate()));
        if (dto.getExpiryDate() != null) referral.setExpiryDate(parseDate(dto.getExpiryDate(), null));
        if (dto.getAuthorizationNumber() != null) referral.setAuthorizationNumber(dto.getAuthorizationNumber());
        if (dto.getInsuranceName() != null) referral.setInsuranceName(dto.getInsuranceName());
        if (dto.getInsuranceId() != null) referral.setInsuranceId(dto.getInsuranceId());
        if (dto.getAppointmentDate() != null) referral.setAppointmentDate(parseDate(dto.getAppointmentDate(), null));
        if (dto.getAppointmentNotes() != null) referral.setAppointmentNotes(dto.getAppointmentNotes());
        if (dto.getFollowUpNotes() != null) referral.setFollowUpNotes(dto.getFollowUpNotes());

        return toDto(repo.save(referral));
    }

    @Transactional
    public ReferralDto updateStatus(Long id, String status) {
        var referral = repo.findById(id)
                .filter(r -> r.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Referral not found: " + id));
        referral.setStatus(status);
        return toDto(repo.save(referral));
    }

    @Transactional
    public void delete(Long id) {
        var referral = repo.findById(id)
                .filter(r -> r.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Referral not found: " + id));
        repo.delete(referral);
    }

    // ── Stats ──

    @Transactional(readOnly = true)
    public Map<String, Long> getStats() {
        String org = orgAlias();
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("draft", repo.countByOrgAliasAndStatus(org, "draft"));
        stats.put("sent", repo.countByOrgAliasAndStatus(org, "sent"));
        stats.put("acknowledged", repo.countByOrgAliasAndStatus(org, "acknowledged"));
        stats.put("scheduled", repo.countByOrgAliasAndStatus(org, "scheduled"));
        stats.put("completed", repo.countByOrgAliasAndStatus(org, "completed"));
        stats.put("cancelled", repo.countByOrgAliasAndStatus(org, "cancelled"));
        stats.put("denied", repo.countByOrgAliasAndStatus(org, "denied"));
        return stats;
    }

    // ── Date parsing ──

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
            // Handle ISO instant like "2026-02-22T03:03:59.059Z"
            if (s.contains("T")) return Instant.parse(s).atZone(ZoneId.systemDefault()).toLocalDate();
            // Handle DD-MM-YYYY
            if (s.matches("\\d{2}-\\d{2}-\\d{4}")) return LocalDate.parse(s, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            // Handle YYYY-MM-DD
            return LocalDate.parse(s);
        } catch (Exception e) {
            log.warn("Failed to parse date '{}', using fallback", s);
            return fallback;
        }
    }

    // ── Mapping ──

    private ReferralDto toDto(Referral e) {
        return ReferralDto.builder()
                .id(e.getId())
                .patientId(e.getPatientId())
                .patientName(e.getPatientName())
                .referringProvider(e.getReferringProvider())
                .specialistName(e.getSpecialistName())
                .specialistNpi(e.getSpecialistNpi())
                .specialty(e.getSpecialty())
                .facilityName(e.getFacilityName())
                .facilityAddress(e.getFacilityAddress())
                .facilityPhone(e.getFacilityPhone())
                .facilityFax(e.getFacilityFax())
                .reason(e.getReason())
                .clinicalNotes(e.getClinicalNotes())
                .urgency(e.getUrgency())
                .status(e.getStatus())
                .referralDate(e.getReferralDate() != null ? e.getReferralDate().toString() : null)
                .expiryDate(e.getExpiryDate() != null ? e.getExpiryDate().toString() : null)
                .authorizationNumber(e.getAuthorizationNumber())
                .insuranceName(e.getInsuranceName())
                .insuranceId(e.getInsuranceId())
                .appointmentDate(e.getAppointmentDate() != null ? e.getAppointmentDate().toString() : null)
                .appointmentNotes(e.getAppointmentNotes())
                .followUpNotes(e.getFollowUpNotes())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }
}
