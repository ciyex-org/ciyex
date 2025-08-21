package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ProviderSignatureDto;
import com.qiaben.ciyex.entity.ProviderSignature;
import com.qiaben.ciyex.repository.ProviderSignatureRepository;
import com.qiaben.ciyex.storage.ExternalProviderSignatureStorage;
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
public class ProviderSignatureService {

    private final ProviderSignatureRepository repo;
    private final Optional<ExternalProviderSignatureStorage> external;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ProviderSignatureDto create(Long orgId, Long patientId, Long encounterId, ProviderSignatureDto in) {
        ProviderSignature e = ProviderSignature.builder()
                .orgId(orgId).patientId(patientId).encounterId(encounterId)
                .signedAt(in.getSignedAt())
                .signedBy(in.getSignedBy())
                .signerRole(in.getSignerRole())
                .signatureType(in.getSignatureType())
                .signatureFormat(in.getSignatureFormat())
                .signatureData(in.getSignatureData())
                .signatureHash(in.getSignatureHash())
                .status(in.getStatus())
                .comments(in.getComments())
                .build();

        final ProviderSignature saved = repo.save(e);

        external.ifPresent(ext -> {
            final ProviderSignature ref = saved;
            String extId = ext.create(mapToDto(ref));
            ref.setExternalId(extId);
            repo.save(ref);
        });

        return mapToDto(saved);
    }

    public ProviderSignatureDto update(Long orgId, Long patientId, Long encounterId, Long id, ProviderSignatureDto in) {
        ProviderSignature e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Provider signature not found"));

        e.setSignedAt(in.getSignedAt());
        e.setSignedBy(in.getSignedBy());
        e.setSignerRole(in.getSignerRole());
        e.setSignatureType(in.getSignatureType());
        e.setSignatureFormat(in.getSignatureFormat());
        e.setSignatureData(in.getSignatureData());
        e.setSignatureHash(in.getSignatureHash());
        e.setStatus(in.getStatus());
        e.setComments(in.getComments());

        final ProviderSignature updated = repo.save(e);

        external.ifPresent(ext -> {
            final ProviderSignature ref = updated;
            if (ref.getExternalId() != null) {
                ext.update(ref.getExternalId(), mapToDto(ref));
            }
        });

        return mapToDto(updated);
    }

    public void delete(Long orgId, Long patientId, Long encounterId, Long id) {
        ProviderSignature e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Provider signature not found"));

        external.ifPresent(ext -> {
            if (e.getExternalId() != null) ext.delete(e.getExternalId());
        });

        repo.delete(e);
    }

    public ProviderSignatureDto getOne(Long orgId, Long patientId, Long encounterId, Long id) {
        ProviderSignature e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Provider signature not found"));
        return mapToDto(e);
    }

    public List<ProviderSignatureDto> getAllByPatient(Long orgId, Long patientId) {
        return repo.findByOrgIdAndPatientId(orgId, patientId).stream().map(this::mapToDto).toList();
    }

    public List<ProviderSignatureDto> getAllByEncounter(Long orgId, Long patientId, Long encounterId) {
        return repo.findByOrgIdAndPatientIdAndEncounterId(orgId, patientId, encounterId).stream().map(this::mapToDto).toList();
    }

    private ProviderSignatureDto mapToDto(ProviderSignature e) {
        ProviderSignatureDto dto = new ProviderSignatureDto();
        dto.setId(e.getId());
        dto.setExternalId(e.getExternalId());
        dto.setOrgId(e.getOrgId());
        dto.setPatientId(e.getPatientId());
        dto.setEncounterId(e.getEncounterId());
        dto.setSignedAt(e.getSignedAt());
        dto.setSignedBy(e.getSignedBy());
        dto.setSignerRole(e.getSignerRole());
        dto.setSignatureType(e.getSignatureType());
        dto.setSignatureFormat(e.getSignatureFormat());
        dto.setSignatureData(e.getSignatureData());
        dto.setSignatureHash(e.getSignatureHash());
        dto.setStatus(e.getStatus());
        dto.setComments(e.getComments());

        ProviderSignatureDto.Audit a = new ProviderSignatureDto.Audit();
        if (e.getCreatedAt() != null) a.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
        if (e.getUpdatedAt() != null) a.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
        dto.setAudit(a);
        return dto;
    }
}
