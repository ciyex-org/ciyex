package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.LabResultDto;
import com.qiaben.ciyex.entity.LabResult;
import com.qiaben.ciyex.repository.LabResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabResultService {

    private final LabResultRepository repository;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // ---------- CRUD ----------
    @Transactional(readOnly = true)
    public LabResultDto getOne(Long id) {
        return repository.findById(id).map(this::toDto).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<LabResultDto> getAll() {
        return repository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LabResultDto> getForPatient(Long patientId) {
        return repository.findByPatientId(patientId).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public LabResultDto create(LabResultDto dto) {
        LabResult e = new LabResult();
        copy(dto, e, true);
        e.setCreatedDate(now());
        e.setLastModifiedDate(e.getCreatedDate());
        repository.save(e);
        log.debug("Created LabResult id={} patientId={} testName={}", e.getId(), e.getPatientId(), e.getTestName());
        return toDto(e);
    }

    @Transactional
    public LabResultDto update(Long id, LabResultDto dto) {
        LabResult e = repository.findById(id).orElse(null);
        if (e == null) return null;
        copy(dto, e, false);
        e.setLastModifiedDate(now());
        repository.save(e);
        log.debug("Updated LabResult id={}", id);
        return toDto(e);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    // ---------- SEARCH (in-memory simple filter; can optimize later) ----------
    @Transactional(readOnly = true)
    public List<LabResultDto> search(String q) {
        String qq = (q == null ? "" : q).trim().toLowerCase();
        return repository.findAll().stream()
                .filter(e -> {
                    String code = safe(e.getCode());
                    String testName = safe(e.getTestName());
                    String specimen = safe(e.getSpecimen());
                    String status = safe(e.getStatus());
                    String abnormal = safe(e.getAbnormalFlag());
                    String value = safe(e.getValue());
                    String procedureName = safe(e.getProcedureName());
                    return code.contains(qq) || testName.contains(qq) || specimen.contains(qq) ||
                           status.contains(qq) || abnormal.contains(qq) || value.contains(qq) ||
                           procedureName.contains(qq) || Objects.equals(String.valueOf(e.getPatientId()), qq);
                })
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ---------- Helpers ----------
    private String safe(String s) { return (s == null ? "" : s).toLowerCase(); }
    private String now() { return LocalDateTime.now().format(TS); }

    private LabResultDto toDto(LabResult e) {
        if (e == null) return null;
        LabResultDto d = new LabResultDto();
        d.setId(e.getId());
    d.setExternalId(e.getExternalId());
        d.setPatientId(e.getPatientId());
        d.setOrderDate(e.getOrderDate());
        d.setProcedureName(e.getProcedureName());
        d.setReportedDate(e.getReportedDate());
        d.setCollectedDate(e.getCollectedDate());
        d.setSpecimen(e.getSpecimen());
        d.setStatus(e.getStatus());
        d.setCode(e.getCode());
        d.setTestName(e.getTestName());
        d.setResultDate(e.getResultDate());
        d.setEndDate(e.getEndDate());
        d.setAbnormalFlag(e.getAbnormalFlag());
        d.setValue(e.getValue());
        d.setUnits(e.getUnits());
        d.setReferenceRange(e.getReferenceRange());
        d.setRecommendations(e.getRecommendations());
        LabResultDto.Audit a = new LabResultDto.Audit();
        a.setCreatedDate(e.getCreatedDate());
        a.setLastModifiedDate(e.getLastModifiedDate());
        d.setAudit(a);
        return d;
    }

    private void copy(LabResultDto d, LabResult e, boolean creating) {
        if (d.getPatientId() != null) e.setPatientId(d.getPatientId());
        if (d.getOrderDate() != null) e.setOrderDate(d.getOrderDate());
        if (d.getProcedureName() != null) e.setProcedureName(d.getProcedureName());
        if (d.getReportedDate() != null) e.setReportedDate(d.getReportedDate());
        if (d.getCollectedDate() != null) e.setCollectedDate(d.getCollectedDate());
        if (d.getSpecimen() != null) e.setSpecimen(d.getSpecimen());
        if (d.getStatus() != null) e.setStatus(d.getStatus());
        if (d.getCode() != null) e.setCode(d.getCode());
        if (d.getTestName() != null) e.setTestName(d.getTestName());
        if (d.getResultDate() != null) e.setResultDate(d.getResultDate());
        if (d.getEndDate() != null) e.setEndDate(d.getEndDate());
        if (d.getAbnormalFlag() != null) e.setAbnormalFlag(d.getAbnormalFlag());
        if (d.getValue() != null) e.setValue(d.getValue());
        if (d.getUnits() != null) e.setUnits(d.getUnits());
        if (d.getReferenceRange() != null) e.setReferenceRange(d.getReferenceRange());
        if (d.getRecommendations() != null) e.setRecommendations(d.getRecommendations());
    if (d.getExternalId() != null) e.setExternalId(d.getExternalId());
    }
}
