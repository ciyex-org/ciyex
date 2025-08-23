package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.PastMedicalHistoryDto;
import com.qiaben.ciyex.entity.PastMedicalHistory;
import com.qiaben.ciyex.repository.PastMedicalHistoryRepository;
import com.qiaben.ciyex.storage.ExternalPastMedicalHistoryStorage;
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
public class PastMedicalHistoryService {

    private final PastMedicalHistoryRepository repo;
    private final Optional<ExternalPastMedicalHistoryStorage> external; // make external optional if not always used

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // CREATE
    public PastMedicalHistoryDto create(Long orgId, Long patientId, Long encounterId, PastMedicalHistoryDto in) {
        PastMedicalHistory toSave = PastMedicalHistory.builder()
                .orgId(orgId)
                .patientId(patientId)
                .encounterId(encounterId)
                .description(in.getDescription())
                .build();

        final PastMedicalHistory saved = repo.save(toSave);

        external.ifPresent(ext -> {
            final PastMedicalHistory ref = saved;
            String externalId = ext.create(mapToDto(ref));
            ref.setExternalId(externalId);
            repo.save(ref);
        });

        return mapToDto(saved);
    }

    // UPDATE
    public PastMedicalHistoryDto update(Long orgId, Long patientId, Long encounterId, Long id, PastMedicalHistoryDto in) {
        PastMedicalHistory entity = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("PMH not found"));

        entity.setDescription(in.getDescription());
        final PastMedicalHistory updated = repo.save(entity);

        external.ifPresent(ext -> {
            final PastMedicalHistory e = updated;
            if (e.getExternalId() != null) {
                ext.update(e.getExternalId(), mapToDto(e));
            }
        });

        return mapToDto(updated);
    }

    // DELETE
    public void delete(Long orgId, Long patientId, Long encounterId, Long id) {
        PastMedicalHistory entity = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("PMH not found"));

        final PastMedicalHistory toDelete = entity;
        external.ifPresent(ext -> {
            if (toDelete.getExternalId() != null) {
                ext.delete(toDelete.getExternalId());
            }
        });

        repo.delete(toDelete);
    }

    // GET ONE
    public PastMedicalHistoryDto getOne(Long orgId, Long patientId, Long encounterId, Long id) {
        PastMedicalHistory entity = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("PMH not found"));
        return mapToDto(entity);
    }

    // GET ALL by patient
    public List<PastMedicalHistoryDto> getAllByPatient(Long orgId, Long patientId) {
        return repo.findByOrgIdAndPatientId(orgId, patientId).stream().map(this::mapToDto).toList();
    }

    // GET ALL by patient + encounter
    public List<PastMedicalHistoryDto> getAllByEncounter(Long orgId, Long patientId, Long encounterId) {
        return repo.findByOrgIdAndPatientIdAndEncounterId(orgId, patientId, encounterId).stream().map(this::mapToDto).toList();
    }

    // Mapping
    private PastMedicalHistoryDto mapToDto(PastMedicalHistory e) {
        PastMedicalHistoryDto dto = new PastMedicalHistoryDto();
        dto.setId(e.getId());
        dto.setExternalId(e.getExternalId());
        dto.setOrgId(e.getOrgId());
        dto.setPatientId(e.getPatientId());
        dto.setEncounterId(e.getEncounterId());
        dto.setDescription(e.getDescription());

        PastMedicalHistoryDto.Audit audit = new PastMedicalHistoryDto.Audit();
        if (e.getCreatedAt() != null) {
            audit.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
        }
        if (e.getUpdatedAt() != null) {
            audit.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
        }
        dto.setAudit(audit);
        return dto;
    }
}
