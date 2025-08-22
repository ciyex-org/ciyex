package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.DateTimeFinalizedDto;
import com.qiaben.ciyex.entity.DateTimeFinalized;
import com.qiaben.ciyex.repository.DateTimeFinalizedRepository;
import com.qiaben.ciyex.storage.ExternalDateTimeFinalizedStorage;
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
public class DateTimeFinalizedService {

    private final DateTimeFinalizedRepository repo;
    private final Optional<ExternalDateTimeFinalizedStorage> external;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public DateTimeFinalizedDto create(Long orgId, Long patientId, Long encounterId, DateTimeFinalizedDto in) {
        DateTimeFinalized e = DateTimeFinalized.builder()
                .orgId(orgId).patientId(patientId).encounterId(encounterId)
                .targetType(in.getTargetType())
                .targetId(in.getTargetId())
                .targetVersion(in.getTargetVersion())
                .finalizedAt(in.getFinalizedAt())
                .finalizedBy(in.getFinalizedBy())
                .finalizerRole(in.getFinalizerRole())
                .method(in.getMethod())
                .status(in.getStatus())
                .reason(in.getReason())
                .comments(in.getComments())
                .contentHash(in.getContentHash())
                .providerSignatureId(in.getProviderSignatureId())
                .signoffId(in.getSignoffId())
                .build();

        final DateTimeFinalized saved = repo.save(e);

        external.ifPresent(ext -> {
            final DateTimeFinalized ref = saved;
            String extId = ext.create(mapToDto(ref));
            ref.setExternalId(extId);
            repo.save(ref);
        });

        return mapToDto(saved);
    }

    public DateTimeFinalizedDto update(Long orgId, Long patientId, Long encounterId, Long id, DateTimeFinalizedDto in) {
        DateTimeFinalized e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Finalization record not found"));

        e.setTargetType(in.getTargetType());
        e.setTargetId(in.getTargetId());
        e.setTargetVersion(in.getTargetVersion());
        e.setFinalizedAt(in.getFinalizedAt());
        e.setFinalizedBy(in.getFinalizedBy());
        e.setFinalizerRole(in.getFinalizerRole());
        e.setMethod(in.getMethod());
        e.setStatus(in.getStatus());
        e.setReason(in.getReason());
        e.setComments(in.getComments());
        e.setContentHash(in.getContentHash());
        e.setProviderSignatureId(in.getProviderSignatureId());
        e.setSignoffId(in.getSignoffId());

        final DateTimeFinalized updated = repo.save(e);

        external.ifPresent(ext -> {
            final DateTimeFinalized ref = updated;
            if (ref.getExternalId() != null) {
                ext.update(ref.getExternalId(), mapToDto(ref));
            }
        });

        return mapToDto(updated);
    }

    public void delete(Long orgId, Long patientId, Long encounterId, Long id) {
        DateTimeFinalized e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Finalization record not found"));

        external.ifPresent(ext -> {
            if (e.getExternalId() != null) ext.delete(e.getExternalId());
        });

        repo.delete(e);
    }

    public DateTimeFinalizedDto getOne(Long orgId, Long patientId, Long encounterId, Long id) {
        DateTimeFinalized e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Finalization record not found"));
        return mapToDto(e);
    }

    public List<DateTimeFinalizedDto> getAllByPatient(Long orgId, Long patientId) {
        return repo.findByOrgIdAndPatientId(orgId, patientId).stream().map(this::mapToDto).toList();
    }

    public List<DateTimeFinalizedDto> getAllByEncounter(Long orgId, Long patientId, Long encounterId) {
        return repo.findByOrgIdAndPatientIdAndEncounterId(orgId, patientId, encounterId).stream().map(this::mapToDto).toList();
    }

    private DateTimeFinalizedDto mapToDto(DateTimeFinalized e) {
        DateTimeFinalizedDto dto = new DateTimeFinalizedDto();
        dto.setId(e.getId());
        dto.setExternalId(e.getExternalId());
        dto.setOrgId(e.getOrgId());
        dto.setPatientId(e.getPatientId());
        dto.setEncounterId(e.getEncounterId());
        dto.setTargetType(e.getTargetType());
        dto.setTargetId(e.getTargetId());
        dto.setTargetVersion(e.getTargetVersion());
        dto.setFinalizedAt(e.getFinalizedAt());
        dto.setFinalizedBy(e.getFinalizedBy());
        dto.setFinalizerRole(e.getFinalizerRole());
        dto.setMethod(e.getMethod());
        dto.setStatus(e.getStatus());
        dto.setReason(e.getReason());
        dto.setComments(e.getComments());
        dto.setContentHash(e.getContentHash());
        dto.setProviderSignatureId(e.getProviderSignatureId());
        dto.setSignoffId(e.getSignoffId());

        DateTimeFinalizedDto.Audit a = new DateTimeFinalizedDto.Audit();
        if (e.getCreatedAt() != null) a.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
        if (e.getUpdatedAt() != null) a.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
        dto.setAudit(a);
        return dto;
    }
}
