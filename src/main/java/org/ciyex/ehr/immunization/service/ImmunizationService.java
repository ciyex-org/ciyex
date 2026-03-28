package org.ciyex.ehr.immunization.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.immunization.dto.ImmunizationDto;
import org.ciyex.ehr.immunization.entity.ImmunizationRecord;
import org.ciyex.ehr.immunization.repository.ImmunizationRepository;
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
public class ImmunizationService {

    private final ImmunizationRepository repo;

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    // ── CRUD ──

    @Transactional
    public ImmunizationDto create(ImmunizationDto dto) {
        var record = ImmunizationRecord.builder()
                .patientId(dto.getPatientId())
                .patientName(dto.getPatientName())
                .vaccineName(dto.getVaccineName())
                .cvxCode(dto.getCvxCode())
                .lotNumber(dto.getLotNumber())
                .manufacturer(dto.getManufacturer())
                .administrationDate(parseDate(dto.getAdministrationDate()))
                .expirationDate(parseDate(dto.getExpirationDate(), null))
                .site(dto.getSite())
                .route(dto.getRoute())
                .doseNumber(dto.getDoseNumber())
                .doseSeries(dto.getDoseSeries())
                .administeredBy(dto.getAdministeredBy())
                .orderingProvider(dto.getOrderingProvider())
                .status(dto.getStatus() != null ? dto.getStatus() : "completed")
                .refusalReason(dto.getRefusalReason())
                .reaction(dto.getReaction())
                .visDate(parseDate(dto.getVisDate(), null))
                .notes(dto.getNotes())
                .orgAlias(orgAlias())
                .build();
        record = repo.save(record);
        return toDto(record);
    }

    @Transactional(readOnly = true)
    public ImmunizationDto getById(Long id) {
        return repo.findById(id)
                .filter(r -> r.getOrgAlias().equals(orgAlias()))
                .map(this::toDto)
                .orElseThrow(() -> new NoSuchElementException("Immunization record not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<ImmunizationDto> getByPatient(Long patientId) {
        return repo.findByOrgAliasAndPatientIdOrderByAdministrationDateDesc(orgAlias(), patientId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ImmunizationDto> getByPatientAndCvx(Long patientId, String cvxCode) {
        return repo.findByOrgAliasAndPatientIdAndCvxCodeOrderByAdministrationDateDesc(orgAlias(), patientId, cvxCode)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Page<ImmunizationDto> getAll(Pageable pageable) {
        return repo.findByOrgAlias(orgAlias(), pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<ImmunizationDto> search(String query) {
        if (query == null || query.isBlank()) {
            return repo.findByOrgAlias(orgAlias(), Pageable.unpaged())
                    .stream().map(this::toDto).toList();
        }
        return repo.search(orgAlias(), query.trim())
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public ImmunizationDto update(Long id, ImmunizationDto dto) {
        var record = repo.findById(id)
                .filter(r -> r.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Immunization record not found: " + id));

        if (dto.getPatientName() != null) record.setPatientName(dto.getPatientName());
        if (dto.getVaccineName() != null) record.setVaccineName(dto.getVaccineName());
        if (dto.getCvxCode() != null) record.setCvxCode(dto.getCvxCode());
        if (dto.getLotNumber() != null) record.setLotNumber(dto.getLotNumber());
        if (dto.getManufacturer() != null) record.setManufacturer(dto.getManufacturer());
        if (dto.getAdministrationDate() != null) record.setAdministrationDate(parseDate(dto.getAdministrationDate()));
        if (dto.getExpirationDate() != null) record.setExpirationDate(parseDate(dto.getExpirationDate(), null));
        if (dto.getSite() != null) record.setSite(dto.getSite());
        if (dto.getRoute() != null) record.setRoute(dto.getRoute());
        if (dto.getDoseNumber() != null) record.setDoseNumber(dto.getDoseNumber());
        if (dto.getDoseSeries() != null) record.setDoseSeries(dto.getDoseSeries());
        if (dto.getAdministeredBy() != null) record.setAdministeredBy(dto.getAdministeredBy());
        if (dto.getOrderingProvider() != null) record.setOrderingProvider(dto.getOrderingProvider());
        if (dto.getStatus() != null) record.setStatus(dto.getStatus());
        if (dto.getRefusalReason() != null) record.setRefusalReason(dto.getRefusalReason());
        if (dto.getReaction() != null) record.setReaction(dto.getReaction());
        if (dto.getVisDate() != null) record.setVisDate(parseDate(dto.getVisDate(), null));
        if (dto.getNotes() != null) record.setNotes(dto.getNotes());

        return toDto(repo.save(record));
    }

    @Transactional
    public void delete(Long id) {
        var record = repo.findById(id)
                .filter(r -> r.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Immunization record not found: " + id));
        repo.delete(record);
    }

    // ── Stats ──

    @Transactional(readOnly = true)
    public Map<String, Object> getPatientStats(Long patientId) {
        String org = orgAlias();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", repo.countByOrgAliasAndPatientId(org, patientId));
        stats.put("completed", repo.countByOrgAliasAndPatientIdAndStatus(org, patientId, "completed"));
        stats.put("notDone", repo.countByOrgAliasAndPatientIdAndStatus(org, patientId, "not_done"));
        stats.put("enteredInError", repo.countByOrgAliasAndPatientIdAndStatus(org, patientId, "entered_in_error"));
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
            if (s.contains("T")) return Instant.parse(s).atZone(ZoneId.systemDefault()).toLocalDate();
            if (s.matches("\\d{2}-\\d{2}-\\d{4}")) return LocalDate.parse(s, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            return LocalDate.parse(s);
        } catch (Exception e) {
            log.warn("Failed to parse date '{}', using fallback", s);
            return fallback;
        }
    }

    // ── Mapping ──

    private ImmunizationDto toDto(ImmunizationRecord e) {
        return ImmunizationDto.builder()
                .id(e.getId())
                .patientId(e.getPatientId())
                .patientName(e.getPatientName())
                .vaccineName(e.getVaccineName())
                .cvxCode(e.getCvxCode())
                .lotNumber(e.getLotNumber())
                .manufacturer(e.getManufacturer())
                .administrationDate(e.getAdministrationDate() != null ? e.getAdministrationDate().toString() : null)
                .expirationDate(e.getExpirationDate() != null ? e.getExpirationDate().toString() : null)
                .site(e.getSite())
                .route(e.getRoute())
                .doseNumber(e.getDoseNumber())
                .doseSeries(e.getDoseSeries())
                .administeredBy(e.getAdministeredBy())
                .orderingProvider(e.getOrderingProvider())
                .status(e.getStatus())
                .refusalReason(e.getRefusalReason())
                .reaction(e.getReaction())
                .visDate(e.getVisDate() != null ? e.getVisDate().toString() : null)
                .notes(e.getNotes())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }
}
