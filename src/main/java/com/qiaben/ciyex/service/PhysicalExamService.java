package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.PhysicalExamDto;
import com.qiaben.ciyex.entity.PhysicalExam;
import com.qiaben.ciyex.entity.PhysicalExamSection;
import com.qiaben.ciyex.repository.PhysicalExamRepository;
import com.qiaben.ciyex.storage.ExternalPhysicalExamStorage;
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
public class PhysicalExamService {

    private final PhysicalExamRepository repo;
    private final Optional<ExternalPhysicalExamStorage> external;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // CREATE
    public PhysicalExamDto create(Long orgId, Long patientId, Long encounterId, PhysicalExamDto in) {
        PhysicalExam pe = new PhysicalExam();
        pe.setOrgId(orgId);
        pe.setPatientId(patientId);
        pe.setEncounterId(encounterId);

        if (in.getSections() != null) {
            for (var s : in.getSections()) {
                PhysicalExamSection row = PhysicalExamSection.builder()
                        .sectionKey(normalizeSection(s.getSectionKey()))
                        .allNormal(Boolean.TRUE.equals(s.getAllNormal()))
                        .normalText(s.getNormalText())
                        .findings(s.getFindings())
                        .physicalExam(pe)
                        .build();
                pe.getSections().add(row);
            }
        }

        final PhysicalExam saved = repo.save(pe);

        external.ifPresent(ext -> {
            final PhysicalExam ref = saved;
            String externalId = ext.create(mapToDto(ref));
            ref.setExternalId(externalId);
            repo.save(ref);
        });

        return mapToDto(saved);
    }

    // UPDATE (replace sections)
    public PhysicalExamDto update(Long orgId, Long patientId, Long encounterId, Long id, PhysicalExamDto in) {
        PhysicalExam pe = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Physical Exam not found"));

        pe.getSections().clear();
        if (in.getSections() != null) {
            for (var s : in.getSections()) {
                PhysicalExamSection row = PhysicalExamSection.builder()
                        .sectionKey(normalizeSection(s.getSectionKey()))
                        .allNormal(Boolean.TRUE.equals(s.getAllNormal()))
                        .normalText(s.getNormalText())
                        .findings(s.getFindings())
                        .physicalExam(pe)
                        .build();
                pe.getSections().add(row);
            }
        }

        final PhysicalExam updated = repo.save(pe);

        external.ifPresent(ext -> {
            final PhysicalExam ref = updated;
            if (ref.getExternalId() != null) {
                ext.update(ref.getExternalId(), mapToDto(ref));
            }
        });

        return mapToDto(updated);
    }

    // DELETE
    public void delete(Long orgId, Long patientId, Long encounterId, Long id) {
        PhysicalExam pe = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Physical Exam not found"));

        final PhysicalExam toDelete = pe;
        external.ifPresent(ext -> {
            if (toDelete.getExternalId() != null) {
                ext.delete(toDelete.getExternalId());
            }
        });

        repo.delete(toDelete);
    }

    // GET ONE
    public PhysicalExamDto getOne(Long orgId, Long patientId, Long encounterId, Long id) {
        PhysicalExam pe = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Physical Exam not found"));
        return mapToDto(pe);
    }

    // GET ALL by patient
    public List<PhysicalExamDto> getAllByPatient(Long orgId, Long patientId) {
        return repo.findByOrgIdAndPatientId(orgId, patientId).stream().map(this::mapToDto).toList();
    }

    // GET ALL by patient + encounter
    public List<PhysicalExamDto> getAllByEncounter(Long orgId, Long patientId, Long encounterId) {
        return repo.findByOrgIdAndPatientIdAndEncounterId(orgId, patientId, encounterId).stream().map(this::mapToDto).toList();
    }

    // --- mapping helpers ---

    private PhysicalExamDto mapToDto(PhysicalExam pe) {
        PhysicalExamDto dto = new PhysicalExamDto();
        dto.setId(pe.getId());
        dto.setExternalId(pe.getExternalId());
        dto.setOrgId(pe.getOrgId());
        dto.setPatientId(pe.getPatientId());
        dto.setEncounterId(pe.getEncounterId());

        dto.setSections(
                pe.getSections().stream().map(s -> {
                    PhysicalExamDto.SectionDto sd = new PhysicalExamDto.SectionDto();
                    sd.setSectionKey(s.getSectionKey());
                    sd.setAllNormal(s.getAllNormal());
                    sd.setNormalText(s.getNormalText());
                    sd.setFindings(s.getFindings());
                    return sd;
                }).toList()
        );

        PhysicalExamDto.Audit a = new PhysicalExamDto.Audit();
        if (pe.getCreatedAt() != null) a.setCreatedDate(pe.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate().toString());
        if (pe.getUpdatedAt() != null) a.setLastModifiedDate(pe.getUpdatedAt().atZone(ZoneId.systemDefault()).toLocalDate().toString());
        dto.setAudit(a);

        return dto;
    }

    private String normalizeSection(String k) {
        if (k == null) return "OTHER";
        String v = k.trim().toUpperCase().replace(' ', '_');
        return switch (v) {
            case "GENERAL", "HEENT", "NECK", "BREASTS", "CARDIOVASCULAR",
                 "THORAX_BACK", "GASTROINTESTINAL", "GU_FEMALE", "GU_MALE",
                 "MUSCULOSKELETAL", "SKIN", "LYMPHATIC", "NEUROLOGIC", "PSYCHIATRIC" -> v;
            default -> "OTHER";
        };
    }
}
