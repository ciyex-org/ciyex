package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.GlobalCodeDto;
import com.qiaben.ciyex.entity.GlobalCode;
import com.qiaben.ciyex.repository.GlobalCodeRepository;
import com.qiaben.ciyex.storage.ExternalGlobalCodeStorage;
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
public class GlobalCodeService {

    private final GlobalCodeRepository repo;
    private final Optional<ExternalGlobalCodeStorage> external;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public GlobalCodeDto create(Long orgId, GlobalCodeDto in) {
        GlobalCode e = GlobalCode.builder()
                .codeType(in.getCodeType()).code(in.getCode()).modifier(in.getModifier())
                .active(in.getActive())
                .description(in.getDescription()).shortDescription(in.getShortDescription())
                .category(in.getCategory())
                .diagnosisReporting(in.getDiagnosisReporting())
                .serviceReporting(in.getServiceReporting())
                .relateTo(in.getRelateTo())
                .feeStandard(in.getFeeStandard())
                .build();

        final GlobalCode saved = repo.save(e);

        external.ifPresent(ext -> {
            String extId = ext.create(mapToDto(saved));
            saved.setExternalId(extId);
            repo.save(saved);
        });

        return mapToDto(saved);
    }

    public GlobalCodeDto update(Long id, GlobalCodeDto in) {
        GlobalCode e = repo.findById(id)
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

        final GlobalCode updated = repo.save(e);

        external.ifPresent(ext -> {
            if (updated.getExternalId() != null) {
                ext.update(updated.getExternalId(), mapToDto(updated));
            }
        });

        return mapToDto(updated);
    }

    public void delete(Long id) {
        GlobalCode e = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Code not found"));

        external.ifPresent(ext -> {
            if (e.getExternalId() != null) ext.delete(e.getExternalId());
        });
        repo.delete(e);
    }

    public GlobalCodeDto getOne(Long id) {
        return repo.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new IllegalArgumentException("Code not found"));
    }

    public List<GlobalCodeDto> getAll() {
        return repo.findAll().stream().map(this::mapToDto).toList();
    }

    public List<GlobalCodeDto> search(Long orgId, String codeType, Boolean active, String q) {
        return repo.search(orgId, codeType, active, q).stream().map(this::mapToDto).toList();
    }

    private GlobalCodeDto mapToDto(GlobalCode e) {
        GlobalCodeDto dto = new GlobalCodeDto();
        dto.setId(e.getId());
        dto.setExternalId(e.getExternalId());
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

        GlobalCodeDto.Audit a = new GlobalCodeDto.Audit();
        if (e.getCreatedAt() != null) a.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
        if (e.getUpdatedAt() != null) a.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
        dto.setAudit(a);
        return dto;
    }
}
