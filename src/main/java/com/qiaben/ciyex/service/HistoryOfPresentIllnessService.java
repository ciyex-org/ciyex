package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.HistoryOfPresentIllnessDto;
import com.qiaben.ciyex.entity.HistoryOfPresentIllness;
import com.qiaben.ciyex.repository.HistoryOfPresentIllnessRepository;
import com.qiaben.ciyex.storage.ExternalHistoryOfPresentIllnessStorage;
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
public class HistoryOfPresentIllnessService {

    private final HistoryOfPresentIllnessRepository repo;
    private final Optional<ExternalHistoryOfPresentIllnessStorage> external;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // CREATE
    public HistoryOfPresentIllnessDto create(Long orgId, Long patientId, Long encounterId, HistoryOfPresentIllnessDto in) {
        HistoryOfPresentIllness toSave = HistoryOfPresentIllness.builder()
                .orgId(orgId)
                .patientId(patientId)
                .encounterId(encounterId)
                .description(in.getDescription())
                .build();

        final HistoryOfPresentIllness saved = repo.save(toSave);

        external.ifPresent(ext -> {
            final HistoryOfPresentIllness ref = saved;
            String externalId = ext.create(mapToDto(ref));
            ref.setExternalId(externalId);
            repo.save(ref);
        });

        return mapToDto(saved);
    }

    // UPDATE
    public HistoryOfPresentIllnessDto update(Long orgId, Long patientId, Long encounterId, Long id, HistoryOfPresentIllnessDto in) {
        HistoryOfPresentIllness entity = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("HPI not found"));

        entity.setDescription(in.getDescription());
        final HistoryOfPresentIllness updated = repo.save(entity);

        external.ifPresent(ext -> {
            final HistoryOfPresentIllness e = updated;
            if (e.getExternalId() != null) {
                ext.update(e.getExternalId(), mapToDto(e));
            }
        });

        return mapToDto(updated);
    }

    // DELETE
    public void delete(Long orgId, Long patientId, Long encounterId, Long id) {
        HistoryOfPresentIllness entity = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("HPI not found"));

        final HistoryOfPresentIllness toDelete = entity;
        external.ifPresent(ext -> {
            if (toDelete.getExternalId() != null) {
                ext.delete(toDelete.getExternalId());
            }
        });

        repo.delete(toDelete);
    }

    // GET ONE
    public HistoryOfPresentIllnessDto getOne(Long orgId, Long patientId, Long encounterId, Long id) {
        HistoryOfPresentIllness entity = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("HPI not found"));
        return mapToDto(entity);
    }

    // GET ALL by patient
    public List<HistoryOfPresentIllnessDto> getAllByPatient(Long orgId, Long patientId) {
        return repo.findByOrgIdAndPatientId(orgId, patientId).stream().map(this::mapToDto).toList();
    }

    // GET ALL by patient + encounter
    public List<HistoryOfPresentIllnessDto> getAllByEncounter(Long orgId, Long patientId, Long encounterId) {
        return repo.findByOrgIdAndPatientIdAndEncounterId(orgId, patientId, encounterId).stream().map(this::mapToDto).toList();
    }

    // Mapping
    private HistoryOfPresentIllnessDto mapToDto(HistoryOfPresentIllness e) {
        HistoryOfPresentIllnessDto dto = new HistoryOfPresentIllnessDto();
        dto.setId(e.getId());
        dto.setExternalId(e.getExternalId());
        dto.setOrgId(e.getOrgId());
        dto.setPatientId(e.getPatientId());
        dto.setEncounterId(e.getEncounterId());
        dto.setDescription(e.getDescription());

        HistoryOfPresentIllnessDto.Audit a = new HistoryOfPresentIllnessDto.Audit();
        if (e.getCreatedAt() != null) {
            a.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
        }
        if (e.getUpdatedAt() != null) {
            a.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
        }
        dto.setAudit(a);
        return dto;
    }
}
