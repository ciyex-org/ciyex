package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.AssessmentDto;
import com.qiaben.ciyex.entity.Assessment;
import com.qiaben.ciyex.repository.AssessmentRepository;
import com.qiaben.ciyex.storage.ExternalAssessmentStorage;
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
public class AssessmentService {

    private final AssessmentRepository repo;
    private final Optional<ExternalAssessmentStorage> external;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // CREATE
    public AssessmentDto create(Long orgId, Long patientId, Long encounterId, AssessmentDto in) {
        Assessment a = Assessment.builder()
                .orgId(orgId)
                .patientId(patientId)
                .encounterId(encounterId)
                .assessmentSummary(in.getAssessmentSummary())
                .planSummary(in.getPlanSummary())
                .notes(in.getNotes())
                .sectionsJson(in.getSectionsJson())
                .build();

        final Assessment saved = repo.save(a);

        external.ifPresent(ext -> {
            final Assessment ref = saved;
            String externalId = ext.create(mapToDto(ref));
            ref.setExternalId(externalId);
            repo.save(ref);
        });

        return mapToDto(saved);
    }

    // UPDATE
    public AssessmentDto update(Long orgId, Long patientId, Long encounterId, Long id, AssessmentDto in) {
        Assessment a = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found"));

        a.setAssessmentSummary(in.getAssessmentSummary());
        a.setPlanSummary(in.getPlanSummary());
        a.setNotes(in.getNotes());
        a.setSectionsJson(in.getSectionsJson());

        final Assessment updated = repo.save(a);

        external.ifPresent(ext -> {
            final Assessment ref = updated;
            if (ref.getExternalId() != null) {
                ext.update(ref.getExternalId(), mapToDto(ref));
            }
        });

        return mapToDto(updated);
    }

    // DELETE
    public void delete(Long orgId, Long patientId, Long encounterId, Long id) {
        Assessment a = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found"));

        final Assessment toDelete = a;
        external.ifPresent(ext -> {
            if (toDelete.getExternalId() != null) {
                ext.delete(toDelete.getExternalId());
            }
        });

        repo.delete(toDelete);
    }

    // GET ONE
    public AssessmentDto getOne(Long orgId, Long patientId, Long encounterId, Long id) {
        Assessment a = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found"));
        return mapToDto(a);
    }

    // GET ALL by patient
    public List<AssessmentDto> getAllByPatient(Long orgId, Long patientId) {
        return repo.findByOrgIdAndPatientId(orgId, patientId).stream().map(this::mapToDto).toList();
    }

    // GET ALL by patient + encounter
    public List<AssessmentDto> getAllByEncounter(Long orgId, Long patientId, Long encounterId) {
        return repo.findByOrgIdAndPatientIdAndEncounterId(orgId, patientId, encounterId).stream().map(this::mapToDto).toList();
    }

    // Mapping
    private AssessmentDto mapToDto(Assessment e) {
        AssessmentDto dto = new AssessmentDto();
        dto.setId(e.getId());
        dto.setExternalId(e.getExternalId());
        dto.setOrgId(e.getOrgId());
        dto.setPatientId(e.getPatientId());
        dto.setEncounterId(e.getEncounterId());
        dto.setAssessmentSummary(e.getAssessmentSummary());
        dto.setPlanSummary(e.getPlanSummary());
        dto.setNotes(e.getNotes());
        dto.setSectionsJson(e.getSectionsJson());

        AssessmentDto.Audit a = new AssessmentDto.Audit();
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
