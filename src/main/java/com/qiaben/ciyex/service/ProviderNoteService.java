package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ProviderNoteDto;
import com.qiaben.ciyex.entity.ProviderNote;
import com.qiaben.ciyex.repository.ProviderNoteRepository;
import com.qiaben.ciyex.storage.ExternalProviderNoteStorage;
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
public class ProviderNoteService {

    private final ProviderNoteRepository repo;
    private final Optional<ExternalProviderNoteStorage> external;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ProviderNoteDto create(Long orgId, Long patientId, Long encounterId, ProviderNoteDto in) {
        ProviderNote e = ProviderNote.builder()
                .orgId(orgId).patientId(patientId).encounterId(encounterId)
                .noteTitle(in.getNoteTitle())
                .noteTypeCode(in.getNoteTypeCode())
                .noteStatus(in.getNoteStatus())
                .authorPractitionerId(in.getAuthorPractitionerId())
                .noteDateTime(in.getNoteDateTime())
                .narrative(in.getNarrative())
                .sectionsJson(in.getSectionsJson())
                .build();

        final ProviderNote saved = repo.save(e);

        external.ifPresent(ext -> {
            final ProviderNote ref = saved; // effectively final
            String externalId = ext.create(mapToDto(ref));
            ref.setExternalId(externalId);
            repo.save(ref);
        });

        return mapToDto(saved);
    }

    public ProviderNoteDto update(Long orgId, Long patientId, Long encounterId, Long id, ProviderNoteDto in) {
        ProviderNote e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Provider note not found"));

        e.setNoteTitle(in.getNoteTitle());
        e.setNoteTypeCode(in.getNoteTypeCode());
        e.setNoteStatus(in.getNoteStatus());
        e.setAuthorPractitionerId(in.getAuthorPractitionerId());
        e.setNoteDateTime(in.getNoteDateTime());
        e.setNarrative(in.getNarrative());
        e.setSectionsJson(in.getSectionsJson());

        final ProviderNote updated = repo.save(e);

        external.ifPresent(ext -> {
            final ProviderNote ref = updated;
            if (ref.getExternalId() != null) {
                ext.update(ref.getExternalId(), mapToDto(ref));
            }
        });

        return mapToDto(updated);
    }

    public void delete(Long orgId, Long patientId, Long encounterId, Long id) {
        ProviderNote e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Provider note not found"));

        final ProviderNote toDelete = e;
        external.ifPresent(ext -> {
            if (toDelete.getExternalId() != null) ext.delete(toDelete.getExternalId());
        });

        repo.delete(toDelete);
    }

    public ProviderNoteDto getOne(Long orgId, Long patientId, Long encounterId, Long id) {
        ProviderNote e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Provider note not found"));
        return mapToDto(e);
    }

    public List<ProviderNoteDto> getAllByPatient(Long orgId, Long patientId) {
        return repo.findByOrgIdAndPatientId(orgId, patientId).stream().map(this::mapToDto).toList();
    }

    public List<ProviderNoteDto> getAllByEncounter(Long orgId, Long patientId, Long encounterId) {
        return repo.findByOrgIdAndPatientIdAndEncounterId(orgId, patientId, encounterId)
                .stream().map(this::mapToDto).toList();
    }

    private ProviderNoteDto mapToDto(ProviderNote e) {
        ProviderNoteDto dto = new ProviderNoteDto();
        dto.setId(e.getId());
        dto.setExternalId(e.getExternalId());
        dto.setOrgId(e.getOrgId());
        dto.setPatientId(e.getPatientId());
        dto.setEncounterId(e.getEncounterId());
        dto.setNoteTitle(e.getNoteTitle());
        dto.setNoteTypeCode(e.getNoteTypeCode());
        dto.setNoteStatus(e.getNoteStatus());
        dto.setAuthorPractitionerId(e.getAuthorPractitionerId());
        dto.setNoteDateTime(e.getNoteDateTime());
        dto.setNarrative(e.getNarrative());
        dto.setSectionsJson(e.getSectionsJson());

        ProviderNoteDto.Audit a = new ProviderNoteDto.Audit();
        if (e.getCreatedAt() != null) a.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
        if (e.getUpdatedAt() != null) a.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
        dto.setAudit(a);
        return dto;
    }
}
