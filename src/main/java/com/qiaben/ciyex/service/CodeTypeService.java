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

        external.ifPresent(ext -> {
            String extId = ext.create(mapToDto(saved));
            saved.setExternalId(extId);
            repo.save(saved);
        });

        return mapToDto(saved);
    }

    // UPDATE
    public CodeTypeDto update(Long patientId, Long encounterId, Long id, CodeTypeDto in) {
        CodeType e = repo.findByPatientIdAndEncounterId(patientId, encounterId).stream()
                .filter(ct -> ct.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("CodeType not found in this encounter"));

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
            if (updated.getExternalId() != null) {
                ext.update(mapToDto(updated), updated.getExternalId());
            }
        });

        return mapToDto(updated);
    }

    // DELETE
    public void delete(Long patientId, Long encounterId, Long id) {
        CodeType e = repo.findByPatientIdAndEncounterId(patientId, encounterId).stream()
                .filter(ct -> ct.getId().equals(id))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("CodeType not found"));

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
                .orElseThrow(() -> new IllegalArgumentException("CodeType not found"));
    }

    // READ ALL by encounter
    public List<CodeTypeDto> getAllByEncounter(Long patientId, Long encounterId) {
        return repo.findByPatientIdAndEncounterId(patientId, encounterId)
                .stream().map(this::mapToDto).toList();
    }

    // SEARCH
    public List<CodeTypeDto> searchInEncounter(Long patientId, Long encounterId,
                                               String codeTypeKey, Boolean active, String q) {
        return repo.searchInEncounter(patientId, encounterId, codeTypeKey, active, q)
                .stream().map(this::mapToDto).toList();
    }

    // --- Mapping helpers ---
    private CodeTypeDto mapToDto(CodeType e) {
        CodeTypeDto dto = new CodeTypeDto();
        dto.setId(e.getId());
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
