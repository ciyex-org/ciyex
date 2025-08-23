package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.PlanDto;
import com.qiaben.ciyex.entity.Plan;
import com.qiaben.ciyex.repository.PlanRepository;
import com.qiaben.ciyex.storage.ExternalPlanStorage;
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
public class PlanService {

    private final PlanRepository repo;
    // wire later if needed; left as Optional to match your original stub
    private final Optional<ExternalPlanStorage> external = Optional.empty();

    private static final DateTimeFormatter DTF = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public PlanDto create(Long orgId, Long patientId, Long encounterId, PlanDto in) {
        Plan p = new Plan();
        p.setOrgId(orgId);
        p.setPatientId(patientId);
        p.setEncounterId(encounterId);
        p.setDiagnosticPlan(in.getDiagnosticPlan());
        p.setPlan(in.getPlan());
        p.setNotes(in.getNotes());
        p.setFollowUpVisit(in.getFollowUpVisit());
        p.setReturnWorkSchool(in.getReturnWorkSchool());
        // ✅ direct JsonNode -> jsonb
        p.setSectionsJson(in.getSectionsJson());

        final Plan saved = repo.save(p);

        external.ifPresent(ext -> {
            String extId = ext.create(mapToDto(saved));
            saved.setExternalId(extId);
            repo.save(saved);
        });

        return mapToDto(saved);
    }

    public PlanDto update(Long orgId, Long patientId, Long encounterId, Long id, PlanDto in) {
        Plan p = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

        p.setDiagnosticPlan(in.getDiagnosticPlan());
        p.setPlan(in.getPlan());
        p.setNotes(in.getNotes());
        p.setFollowUpVisit(in.getFollowUpVisit());
        p.setReturnWorkSchool(in.getReturnWorkSchool());
        // ✅ direct JsonNode -> jsonb
        p.setSectionsJson(in.getSectionsJson());

        final Plan updated = repo.save(p);

        external.ifPresent(ext -> {
            if (updated.getExternalId() != null) {
                ext.update(updated.getExternalId(), mapToDto(updated));
            }
        });

        return mapToDto(updated);
    }

    public void delete(Long orgId, Long patientId, Long encounterId, Long id) {
        Plan p = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));
        repo.delete(p);
        external.ifPresent(ext -> {
            if (p.getExternalId() != null) {
                ext.delete(p.getExternalId());
            }
        });
    }

    public PlanDto getOne(Long orgId, Long patientId, Long encounterId, Long id) {
        Plan p = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));
        return mapToDto(p);
    }

    public List<PlanDto> getAllByPatient(Long orgId, Long patientId) {
        return repo.findByOrgIdAndPatientId(orgId, patientId)
                .stream().map(this::mapToDto).toList();
    }

    public List<PlanDto> getAllByEncounter(Long orgId, Long patientId, Long encounterId) {
        return repo.findByOrgIdAndPatientIdAndEncounterId(orgId, patientId, encounterId)
                .stream().map(this::mapToDto).toList();
    }

    private PlanDto mapToDto(Plan e) {
        PlanDto dto = new PlanDto();
        dto.setId(e.getId());
        dto.setExternalId(e.getExternalId());
        dto.setOrgId(e.getOrgId());
        dto.setPatientId(e.getPatientId());
        dto.setEncounterId(e.getEncounterId());
        dto.setDiagnosticPlan(e.getDiagnosticPlan());
        dto.setPlan(e.getPlan());
        dto.setNotes(e.getNotes());
        dto.setFollowUpVisit(e.getFollowUpVisit());
        dto.setReturnWorkSchool(e.getReturnWorkSchool());
        // ✅ JsonNode passes straight through
        dto.setSectionsJson(e.getSectionsJson());

        PlanDto.Audit a = new PlanDto.Audit();
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
