package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.PatientMedicalHistoryDto;
import com.qiaben.ciyex.entity.PatientMedicalHistory;
import com.qiaben.ciyex.repository.PatientMedicalHistoryRepository;
import com.qiaben.ciyex.storage.ExternalPatientMedicalHistoryStorage;
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
public class PatientMedicalHistoryService {

    private final PatientMedicalHistoryRepository repo;
    private final Optional<ExternalPatientMedicalHistoryStorage> external;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // CREATE
    public PatientMedicalHistoryDto create(Long orgId, Long patientId, Long encounterId, PatientMedicalHistoryDto in) {
        PatientMedicalHistory toSave = PatientMedicalHistory.builder()
                .orgId(orgId)
                .patientId(patientId)
                .encounterId(encounterId)
                .description(in.getDescription())
                .build();

        // persist locally
        final PatientMedicalHistory saved = repo.save(toSave);

        // use final references inside lambda
        external.ifPresent(ext -> {
            final PatientMedicalHistory snapshotRef = saved; // final reference
            String externalId = ext.create(mapToDto(snapshotRef));
            snapshotRef.setExternalId(externalId);
            repo.save(snapshotRef);
        });

        return mapToDto(saved);
    }

    // UPDATE
    public PatientMedicalHistoryDto update(Long orgId, Long patientId, Long encounterId, Long id, PatientMedicalHistoryDto in) {
        PatientMedicalHistory entity = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("PMH not found"));

        entity.setDescription(in.getDescription());
        final PatientMedicalHistory updated = repo.save(entity);

        // capture a final reference for lambda use
        external.ifPresent(ext -> {
            final PatientMedicalHistory e = updated;
            if (e.getExternalId() != null) {
                ext.update(e.getExternalId(), mapToDto(e));
            }
        });

        return mapToDto(updated);
    }

    // DELETE
    public void delete(Long orgId, Long patientId, Long encounterId, Long id) {
        PatientMedicalHistory entity = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("PMH not found"));

        final PatientMedicalHistory toDelete = entity; // final for lambda
        external.ifPresent(ext -> {
            if (toDelete.getExternalId() != null) {
                ext.delete(toDelete.getExternalId());
            }
        });

        repo.delete(toDelete);
    }

    // GET ONE
    public PatientMedicalHistoryDto getOne(Long orgId, Long patientId, Long encounterId, Long id) {
        PatientMedicalHistory entity = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("PMH not found"));
        return mapToDto(entity);
    }

    // GET ALL by patient
    public List<PatientMedicalHistoryDto> getAllByPatient(Long orgId, Long patientId) {
        return repo.findByOrgIdAndPatientId(orgId, patientId)
                .stream().map(this::mapToDto).toList();
    }

    // GET ALL by patient + encounter
    public List<PatientMedicalHistoryDto> getAllByEncounter(Long orgId, Long patientId, Long encounterId) {
        return repo.findByOrgIdAndPatientIdAndEncounterId(orgId, patientId, encounterId)
                .stream().map(this::mapToDto).toList();
    }

    // Mapping
    private PatientMedicalHistoryDto mapToDto(PatientMedicalHistory e) {
        PatientMedicalHistoryDto dto = new PatientMedicalHistoryDto();
        dto.setId(e.getId());
        dto.setExternalId(e.getExternalId());
        dto.setOrgId(e.getOrgId());
        dto.setPatientId(e.getPatientId());
        dto.setEncounterId(e.getEncounterId());
        dto.setDescription(e.getDescription());

        PatientMedicalHistoryDto.Audit audit = new PatientMedicalHistoryDto.Audit();
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
