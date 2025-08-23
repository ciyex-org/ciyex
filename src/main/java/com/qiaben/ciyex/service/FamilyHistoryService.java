package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.FamilyHistoryDto;
import com.qiaben.ciyex.entity.FamilyHistory;
import com.qiaben.ciyex.entity.FamilyHistoryEntry;
import com.qiaben.ciyex.repository.FamilyHistoryRepository;
import com.qiaben.ciyex.storage.ExternalFamilyHistoryStorage;
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
public class FamilyHistoryService {

    private final FamilyHistoryRepository repo;
    private final Optional<ExternalFamilyHistoryStorage> external;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // CREATE
    public FamilyHistoryDto create(Long orgId, Long patientId, Long encounterId, FamilyHistoryDto in) {
        FamilyHistory fh = new FamilyHistory();
        fh.setOrgId(orgId);
        fh.setPatientId(patientId);
        fh.setEncounterId(encounterId);

        // map entries
        if (in.getEntries() != null) {
            for (var e : in.getEntries()) {
                FamilyHistoryEntry row = FamilyHistoryEntry.builder()
                        .relation(normalizeRelation(e.getRelation()))
                        .diagnosisCode(e.getDiagnosisCode())
                        .diagnosisText(e.getDiagnosisText())
                        .notes(e.getNotes())
                        .familyHistory(fh)
                        .build();
                fh.getEntries().add(row);
            }
        }

        final FamilyHistory saved = repo.save(fh);

        external.ifPresent(ext -> {
            final FamilyHistory ref = saved;
            String externalId = ext.create(mapToDto(ref));
            ref.setExternalId(externalId);
            repo.save(ref);
        });

        return mapToDto(saved);
    }

    // UPDATE (replace entries)
    public FamilyHistoryDto update(Long orgId, Long patientId, Long encounterId, Long id, FamilyHistoryDto in) {
        FamilyHistory fh = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Family History not found"));

        // full replace list
        fh.getEntries().clear();
        if (in.getEntries() != null) {
            for (var e : in.getEntries()) {
                FamilyHistoryEntry row = FamilyHistoryEntry.builder()
                        .relation(normalizeRelation(e.getRelation()))
                        .diagnosisCode(e.getDiagnosisCode())
                        .diagnosisText(e.getDiagnosisText())
                        .notes(e.getNotes())
                        .familyHistory(fh)
                        .build();
                fh.getEntries().add(row);
            }
        }

        final FamilyHistory updated = repo.save(fh);

        external.ifPresent(ext -> {
            final FamilyHistory ref = updated;
            if (ref.getExternalId() != null) {
                ext.update(ref.getExternalId(), mapToDto(ref));
            }
        });

        return mapToDto(updated);
    }

    // DELETE
    public void delete(Long orgId, Long patientId, Long encounterId, Long id) {
        FamilyHistory fh = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Family History not found"));

        final FamilyHistory toDelete = fh;
        external.ifPresent(ext -> {
            if (toDelete.getExternalId() != null) {
                ext.delete(toDelete.getExternalId());
            }
        });

        repo.delete(toDelete);
    }

    // GET ONE
    public FamilyHistoryDto getOne(Long orgId, Long patientId, Long encounterId, Long id) {
        FamilyHistory fh = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Family History not found"));
        return mapToDto(fh);
    }

    // GET ALL by patient
    public List<FamilyHistoryDto> getAllByPatient(Long orgId, Long patientId) {
        return repo.findByOrgIdAndPatientId(orgId, patientId).stream().map(this::mapToDto).toList();
    }

    // GET ALL by patient + encounter
    public List<FamilyHistoryDto> getAllByEncounter(Long orgId, Long patientId, Long encounterId) {
        return repo.findByOrgIdAndPatientIdAndEncounterId(orgId, patientId, encounterId).stream().map(this::mapToDto).toList();
    }

    // --- mapping helpers ---

    private FamilyHistoryDto mapToDto(FamilyHistory fh) {
        FamilyHistoryDto dto = new FamilyHistoryDto();
        dto.setId(fh.getId());
        dto.setExternalId(fh.getExternalId());
        dto.setOrgId(fh.getOrgId());
        dto.setPatientId(fh.getPatientId());
        dto.setEncounterId(fh.getEncounterId());

        if (fh.getEntries() != null) {
            dto.setEntries(
                    fh.getEntries().stream().map(e -> {
                        FamilyHistoryDto.EntryDto ed = new FamilyHistoryDto.EntryDto();
                        ed.setId(e.getId());
                        ed.setRelation(e.getRelation());
                        ed.setDiagnosisCode(e.getDiagnosisCode());
                        ed.setDiagnosisText(e.getDiagnosisText());
                        ed.setNotes(e.getNotes());
                        return ed;
                    }).toList()
            );
        }

        FamilyHistoryDto.Audit a = new FamilyHistoryDto.Audit();
        if (fh.getCreatedAt() != null) a.setCreatedDate(fh.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate().toString());
        if (fh.getUpdatedAt() != null) a.setLastModifiedDate(fh.getUpdatedAt().atZone(ZoneId.systemDefault()).toLocalDate().toString());
        dto.setAudit(a);

        return dto;
    }

    private String normalizeRelation(String r) {
        if (r == null) return "OTHER";
        String v = r.trim().toUpperCase();
        return switch (v) {
            case "FATHER", "MOTHER", "SIBLING", "SPOUSE", "OFFSPRING" -> v;
            default -> "OTHER";
        };
    }
}
