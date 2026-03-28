package org.ciyex.ehr.education.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.education.dto.PatientEducationAssignmentDto;
import org.ciyex.ehr.education.entity.PatientEducationAssignment;
import org.ciyex.ehr.education.repository.EducationMaterialRepository;
import org.ciyex.ehr.education.repository.PatientEducationAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientEducationService {

    private final PatientEducationAssignmentRepository repo;
    private final EducationMaterialRepository materialRepo;

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    @Transactional
    public PatientEducationAssignmentDto assign(PatientEducationAssignmentDto dto) {
        // Validate material exists
        var material = materialRepo.findById(dto.getMaterialId())
                .filter(m -> m.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Material not found: " + dto.getMaterialId()));

        var assignment = PatientEducationAssignment.builder()
                .patientId(dto.getPatientId())
                .patientName(dto.getPatientName())
                .materialId(dto.getMaterialId())
                .assignedBy(dto.getAssignedBy())
                .assignedDate(dto.getAssignedDate() != null ? parseDate(dto.getAssignedDate()) : LocalDate.now())
                .dueDate(dto.getDueDate() != null ? parseDate(dto.getDueDate()) : null)
                .status("assigned")
                .encounterId(dto.getEncounterId())
                .notes(dto.getNotes())
                .orgAlias(orgAlias())
                .build();
        assignment = repo.save(assignment);
        return toDto(assignment);
    }

    @Transactional(readOnly = true)
    public List<PatientEducationAssignmentDto> getByPatient(Long patientId) {
        return repo.findByOrgAliasAndPatientIdOrderByAssignedDateDesc(orgAlias(), patientId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PatientEducationAssignmentDto getById(Long id) {
        return repo.findById(id)
                .filter(a -> a.getOrgAlias().equals(orgAlias()))
                .map(this::toDto)
                .orElseThrow(() -> new NoSuchElementException("Assignment not found: " + id));
    }

    @Transactional
    public PatientEducationAssignmentDto markViewed(Long id) {
        var assignment = repo.findById(id)
                .filter(a -> a.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Assignment not found: " + id));
        assignment.setStatus("viewed");
        assignment.setViewedAt(LocalDateTime.now());
        return toDto(repo.save(assignment));
    }

    @Transactional
    public PatientEducationAssignmentDto markCompleted(Long id) {
        var assignment = repo.findById(id)
                .filter(a -> a.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Assignment not found: " + id));
        assignment.setStatus("completed");
        assignment.setCompletedAt(LocalDateTime.now());
        if (assignment.getViewedAt() == null) {
            assignment.setViewedAt(LocalDateTime.now());
        }
        return toDto(repo.save(assignment));
    }

    @Transactional
    public PatientEducationAssignmentDto dismiss(Long id) {
        var assignment = repo.findById(id)
                .filter(a -> a.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Assignment not found: " + id));
        assignment.setStatus("dismissed");
        return toDto(repo.save(assignment));
    }

    @Transactional
    public void delete(Long id) {
        var assignment = repo.findById(id)
                .filter(a -> a.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Assignment not found: " + id));
        repo.delete(assignment);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getPatientStats(Long patientId) {
        String org = orgAlias();
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("assigned", repo.countByOrgAliasAndPatientIdAndStatus(org, patientId, "assigned"));
        stats.put("viewed", repo.countByOrgAliasAndPatientIdAndStatus(org, patientId, "viewed"));
        stats.put("completed", repo.countByOrgAliasAndPatientIdAndStatus(org, patientId, "completed"));
        stats.put("dismissed", repo.countByOrgAliasAndPatientIdAndStatus(org, patientId, "dismissed"));
        return stats;
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return LocalDate.now();
        try {
            if (s.contains("T")) return Instant.parse(s).atZone(ZoneId.systemDefault()).toLocalDate();
            if (s.matches("\\d{2}-\\d{2}-\\d{4}")) return LocalDate.parse(s, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            return LocalDate.parse(s);
        } catch (Exception e) {
            log.warn("Failed to parse date '{}', using today", s);
            return LocalDate.now();
        }
    }

    private PatientEducationAssignmentDto toDto(PatientEducationAssignment e) {
        // Enrich with material info
        String materialTitle = null;
        String materialCategory = null;
        String materialContentType = null;
        if (e.getMaterialId() != null) {
            var material = materialRepo.findById(e.getMaterialId()).orElse(null);
            if (material != null) {
                materialTitle = material.getTitle();
                materialCategory = material.getCategory();
                materialContentType = material.getContentType();
            }
        }

        return PatientEducationAssignmentDto.builder()
                .id(e.getId())
                .patientId(e.getPatientId())
                .patientName(e.getPatientName())
                .materialId(e.getMaterialId())
                .materialTitle(materialTitle)
                .materialCategory(materialCategory)
                .materialContentType(materialContentType)
                .assignedBy(e.getAssignedBy())
                .assignedDate(e.getAssignedDate() != null ? e.getAssignedDate().toString() : null)
                .dueDate(e.getDueDate() != null ? e.getDueDate().toString() : null)
                .status(e.getStatus())
                .viewedAt(e.getViewedAt() != null ? e.getViewedAt().toString() : null)
                .completedAt(e.getCompletedAt() != null ? e.getCompletedAt().toString() : null)
                .encounterId(e.getEncounterId())
                .notes(e.getNotes())
                .patientFeedback(e.getPatientFeedback())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }
}
