package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.CodeDto;
import com.qiaben.ciyex.entity.Code;
import com.qiaben.ciyex.repository.CodeRepository;
import com.qiaben.ciyex.storage.ExternalCodeStorage;
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
public class CodeService {

    private final CodeRepository repo;
    private final Optional<ExternalCodeStorage> external;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public CodeDto create(Long orgId, CodeDto in) {
        Code e = Code.builder()
                .orgId(orgId)
                .codeType(in.getCodeType()).code(in.getCode()).modifier(in.getModifier())
                .active(in.getActive())
                .description(in.getDescription()).shortDescription(in.getShortDescription())
                .category(in.getCategory())
                .diagnosisReporting(in.getDiagnosisReporting())
                .serviceReporting(in.getServiceReporting())
                .relateTo(in.getRelateTo())
                .feeStandard(in.getFeeStandard())
                .build();

        final Code saved = repo.save(e);

        external.ifPresent(ext -> {
            String extId = ext.create(mapToDto(saved));
            saved.setExternalId(extId);
            repo.save(saved);
        });

        return mapToDto(saved);
    }

    public CodeDto update(Long orgId, Long id, CodeDto in) {
        Code e = repo.findById(id)
                .filter(c -> c.getOrgId().equals(orgId))
                .orElseThrow(() -> new IllegalArgumentException("Code not found"));

        e.setCodeType(in.getCodeType());
        e.setCode(in.getCode());
        e.setModifier(in.getModifier());
        e.setActive(in.getActive());
        e.setDescription(in.getDescription());
        e.setShortDescription(in.getShortDescription());
        e.setCategory(in.getCategory());
        e.setDiagnosisReporting(in.getDiagnosisReporting());
        e.setServiceReporting(in.getServiceReporting());
        e.setRelateTo(in.getRelateTo());
        e.setFeeStandard(in.getFeeStandard());

        final Code updated = repo.save(e);

        external.ifPresent(ext -> {
            if (updated.getExternalId() != null) {
                ext.update(updated.getExternalId(), mapToDto(updated));
            }
        });

        return mapToDto(updated);
    }

    public void delete(Long orgId, Long id) {
        Code e = repo.findById(id)
                .filter(c -> c.getOrgId().equals(orgId))
                .orElseThrow(() -> new IllegalArgumentException("Code not found"));

        external.ifPresent(ext -> {
            if (e.getExternalId() != null) ext.delete(e.getExternalId());
        });
        repo.delete(e);
    }

    public CodeDto getOne(Long orgId, Long id) {
        return repo.findById(id)
                .filter(c -> c.getOrgId().equals(orgId))
                .map(this::mapToDto)
                .orElseThrow(() -> new IllegalArgumentException("Code not found"));
    }

    public List<CodeDto> getAll(Long orgId) {
        return repo.findByOrgId(orgId).stream().map(this::mapToDto).toList();
    }

    public List<CodeDto> search(Long orgId, String codeType, Boolean active, String q) {
        return repo.search(orgId, codeType, active, q).stream().map(this::mapToDto).toList();
    }

    private CodeDto mapToDto(Code e) {
        CodeDto dto = new CodeDto();
        dto.setId(e.getId());
        dto.setExternalId(e.getExternalId());
        dto.setOrgId(e.getOrgId());
        dto.setCodeType(e.getCodeType());
        dto.setCode(e.getCode());
        dto.setModifier(e.getModifier());
        dto.setActive(e.getActive());
        dto.setDescription(e.getDescription());
        dto.setShortDescription(e.getShortDescription());
        dto.setCategory(e.getCategory());
        dto.setDiagnosisReporting(e.getDiagnosisReporting());
        dto.setServiceReporting(e.getServiceReporting());
        dto.setRelateTo(e.getRelateTo());
        dto.setFeeStandard(e.getFeeStandard());

        CodeDto.Audit a = new CodeDto.Audit();
        if (e.getCreatedAt() != null) a.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
        if (e.getUpdatedAt() != null) a.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
        dto.setAudit(a);
        return dto;
    }
}
