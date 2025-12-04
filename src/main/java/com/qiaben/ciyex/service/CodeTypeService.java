package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.CodeTypeDto;
import com.qiaben.ciyex.entity.CodeType;
import com.qiaben.ciyex.repository.CodeTypeRepository;
import com.qiaben.ciyex.storage.ExternalStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeTypeService {

    private final CodeTypeRepository repo;
    private final Optional<ExternalStorage<CodeTypeDto>> external;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // CREATE
    public CodeTypeDto create(Long patientId, Long encounterId, CodeTypeDto in) {
                // Validate mandatory fields
                if (in.getCodeTypeKey() == null || in.getCodeTypeKey().trim().isEmpty())
                    throw new IllegalArgumentException("codeTypeKey is required");
                if (in.getCodeTypeId() == null)
                    throw new IllegalArgumentException("codeTypeId is required");
                if (in.getModifier() == null)
                    throw new IllegalArgumentException("modifier is required");
                if (in.getSequenceNumber() == null)
                    throw new IllegalArgumentException("sequenceNumber is required");
                if (in.getJustification() == null || in.getJustification().trim().isEmpty())
                    throw new IllegalArgumentException("justification is required");
                if (in.getMask() == null || in.getMask().trim().isEmpty())
                    throw new IllegalArgumentException("mask is required");
        CodeType e = CodeType.builder()
                .patientId(patientId).encounterId(encounterId)
                .codeTypeKey(in.getCodeTypeKey())
                .codeTypeId(in.getCodeTypeId())
                .sequenceNumber(in.getSequenceNumber())
                .modifier(in.getModifier())
                .justification(in.getJustification())
                .mask(in.getMask())
                .feeApplicable(in.getFeeApplicable())
                .relatedIndicator(in.getRelatedIndicator())
                .numberOfServices(in.getNumberOfServices())
                .diagnosisFlag(in.getDiagnosisFlag())
                .active(in.getActive())
                .label(in.getLabel())
                .externalFlag(in.getExternalFlag())
                .claimFlag(in.getClaimFlag())
                .procedureFlag(in.getProcedureFlag())
                .terminologyFlag(in.getTerminologyFlag())
                .problemFlag(in.getProblemFlag())
                .drugFlag(in.getDrugFlag())
                .build();

        final CodeType saved = repo.save(e);

        // Try to sync with external storage if available; fall back to local generation
        String finalExtId = saved.getExternalId();
        if (external.isPresent()) {
            try {
                String extId = external.get().create(mapToDto(saved));
                if (extId != null) {
                    // Normalize to FHIR id part if a full URL or resource path was returned
                    extId = normalizeFhirId(extId);
                    finalExtId = extId;
                    saved.setFhirId(extId);
                    saved.setExternalId(extId);
                    repo.save(saved);
                    log.info("CodeType created in external storage with externalId={} fhirId={}", extId, extId);
                } else {
                    log.warn("External storage returned null externalId for CodeType id={}", saved.getId());
                }
            } catch (Exception ex) {
                log.warn("External storage create failed for CodeType id={} msg={}", saved.getId(), ex.getMessage());
            }
        }

        // Ensure externalId and fhirId are set (generate if missing)
        if (finalExtId == null) {
            finalExtId = "CT-" + System.currentTimeMillis();
            saved.setFhirId(finalExtId);
            saved.setExternalId(finalExtId);
            repo.save(saved);
            log.info("Auto-generated externalId for CodeType id={} externalId={} fhirId={}", saved.getId(), finalExtId, finalExtId);
        }

        return mapToDto(saved);
    }

    // UPDATE
    public CodeTypeDto update(Long patientId, Long encounterId, Long id, CodeTypeDto in) {
        CodeType e = repo.findByPatientIdAndEncounterId(patientId, encounterId).stream()
                .filter(ct -> ct.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Failed to retrieve codetype: codetype not found for id=" + id + ", patientId=" + patientId + ", encounterId=" + encounterId));

        e.setCodeTypeKey(in.getCodeTypeKey());
        e.setCodeTypeId(in.getCodeTypeId());
        e.setSequenceNumber(in.getSequenceNumber());
        e.setModifier(in.getModifier());
        e.setJustification(in.getJustification());
        e.setMask(in.getMask());
        e.setFeeApplicable(in.getFeeApplicable());
        e.setRelatedIndicator(in.getRelatedIndicator());
        e.setNumberOfServices(in.getNumberOfServices());
        e.setDiagnosisFlag(in.getDiagnosisFlag());
        e.setActive(in.getActive());
        e.setLabel(in.getLabel());
        e.setExternalFlag(in.getExternalFlag());
        e.setClaimFlag(in.getClaimFlag());
        e.setProcedureFlag(in.getProcedureFlag());
        e.setTerminologyFlag(in.getTerminologyFlag());
        e.setProblemFlag(in.getProblemFlag());
        e.setDrugFlag(in.getDrugFlag());

        final CodeType updated = repo.save(e);

        external.ifPresent(ext -> {
            try {
                if (updated.getExternalId() != null) {
                    ext.update(mapToDto(updated), updated.getExternalId());
                } else {
                    // No externalId yet — create in external storage and persist returned FHIR id
                    String extId = ext.create(mapToDto(updated));
                    if (extId != null) {
                        extId = normalizeFhirId(extId);
                        updated.setFhirId(extId);
                        updated.setExternalId(extId);
                        repo.save(updated);
                        log.info("CodeType created in external storage during update with externalId={} fhirId={}", extId, extId);
                    }
                }
            } catch (Exception ex) {
                log.warn("External storage update/create failed for CodeType id={} msg={}", updated.getId(), ex.getMessage());
            }
        });

        return mapToDto(updated);
    }

    // DELETE
    public void delete(Long patientId, Long encounterId, Long id) {
        CodeType e = repo.findByPatientIdAndEncounterId(patientId, encounterId).stream()
                .filter(ct -> ct.getId().equals(id))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Failed to retrieve codetype: codetype not found for id=" + id + ", patientId=" + patientId + ", encounterId=" + encounterId));

        external.ifPresent(ext -> {
            if (e.getExternalId() != null) ext.delete(e.getExternalId());
        });

        repo.delete(e);
    }

    // READ ONE
    public CodeTypeDto getOne(Long patientId, Long encounterId, Long id) {
        return repo.findByPatientIdAndEncounterId(patientId, encounterId).stream()
                .filter(ct -> ct.getId().equals(id))
                .findFirst()
                .map(this::mapToDto)
                .orElseThrow(() -> new IllegalArgumentException("Failed to retrieve codetype: codetype not found for id=" + id + ", patientId=" + patientId + ", encounterId=" + encounterId));
    }

    // READ ALL by encounter
    public List<CodeTypeDto> getAllByEncounter(Long patientId, Long encounterId) {
        List<CodeTypeDto> list = repo.findByPatientIdAndEncounterId(patientId, encounterId)
                .stream().map(this::mapToDto).toList();
        if (list.isEmpty()) {
            throw new IllegalArgumentException("No CodeTypes found for patientId=" + patientId + ", encounterId=" + encounterId);
        }
        return list;
    }

    // SEARCH
    public List<CodeTypeDto> searchInEncounter(Long patientId, Long encounterId,
                                               String codeTypeKey, Boolean active, String q) {
        return repo.searchInEncounter(patientId, encounterId, codeTypeKey, active, q)
                .stream().map(this::mapToDto).toList();
    }

    // --- Mapping helpers ---
    // Normalize FHIR identifiers returned by external storage to the plain resource id
    private String normalizeFhirId(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        // If a URL or resource path was returned, extract the last path segment
        int slash = v.lastIndexOf('/');
        if (slash >= 0 && slash < v.length() - 1) {
            v = v.substring(slash + 1);
        }
        // If the id is of the form "Resource/id/_history/...", remove history parts
        int history = v.indexOf('/');
        if (history >= 0) v = v.substring(0, history);
        // If the id contains a pipe (some systems return "id|version"), take left side
        int pipe = v.indexOf('|');
        if (pipe >= 0) v = v.substring(0, pipe);
        return v;
    }

    private CodeTypeDto mapToDto(CodeType e) {
        CodeTypeDto dto = new CodeTypeDto();
        dto.setId(e.getId());
        dto.setFhirId(e.getFhirId());
        dto.setExternalId(e.getExternalId());
        dto.setPatientId(e.getPatientId());
        dto.setEncounterId(e.getEncounterId());
        dto.setCodeTypeKey(e.getCodeTypeKey());
        dto.setCodeTypeId(e.getCodeTypeId());
        dto.setSequenceNumber(e.getSequenceNumber());
        dto.setModifier(e.getModifier());
        dto.setJustification(e.getJustification());
        dto.setMask(e.getMask());
        dto.setFeeApplicable(e.getFeeApplicable());
        dto.setRelatedIndicator(e.getRelatedIndicator());
        dto.setNumberOfServices(e.getNumberOfServices());
        dto.setDiagnosisFlag(e.getDiagnosisFlag());
        dto.setActive(e.getActive());
        dto.setLabel(e.getLabel());
        dto.setExternalFlag(e.getExternalFlag());
        dto.setClaimFlag(e.getClaimFlag());
        dto.setProcedureFlag(e.getProcedureFlag());
        dto.setTerminologyFlag(e.getTerminologyFlag());
        dto.setProblemFlag(e.getProblemFlag());
        dto.setDrugFlag(e.getDrugFlag());

        CodeTypeDto.Audit a = new CodeTypeDto.Audit();
        if (e.getCreatedAt() != null) a.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
        if (e.getUpdatedAt() != null) a.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
        dto.setAudit(a);
        return dto;
    }
}
