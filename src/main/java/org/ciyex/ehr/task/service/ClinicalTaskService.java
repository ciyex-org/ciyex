package org.ciyex.ehr.task.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.task.dto.ClinicalTaskDto;
import org.ciyex.ehr.task.entity.ClinicalTask;
import org.ciyex.ehr.task.repository.ClinicalTaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClinicalTaskService {

    private final ClinicalTaskRepository repo;

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    @Transactional
    public ClinicalTaskDto create(ClinicalTaskDto dto) {
        var task = ClinicalTask.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .taskType(dto.getTaskType() != null ? dto.getTaskType() : "general")
                .status(dto.getStatus() != null ? dto.getStatus() : "pending")
                .priority(dto.getPriority() != null ? dto.getPriority() : "normal")
                .dueDate(parseDate(dto.getDueDate()))
                .dueTime(parseTime(dto.getDueTime()))
                .assignedTo(dto.getAssignedTo())
                .assignedBy(dto.getAssignedBy())
                .patientId(dto.getPatientId())
                .patientName(dto.getPatientName())
                .encounterId(dto.getEncounterId())
                .referenceType(dto.getReferenceType())
                .referenceId(dto.getReferenceId())
                .notes(dto.getNotes())
                .orgAlias(orgAlias())
                .build();
        task = repo.save(task);
        return toDto(task);
    }

    @Transactional(readOnly = true)
    public ClinicalTaskDto getById(Long id) {
        return repo.findById(id)
                .filter(t -> t.getOrgAlias().equals(orgAlias()))
                .map(this::toDto)
                .orElseThrow(() -> new NoSuchElementException("Task not found: " + id));
    }

    @Transactional
    public ClinicalTaskDto update(Long id, ClinicalTaskDto dto) {
        var task = repo.findById(id)
                .filter(t -> t.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Task not found: " + id));

        if (dto.getTitle() != null) task.setTitle(dto.getTitle());
        if (dto.getDescription() != null) task.setDescription(dto.getDescription());
        if (dto.getTaskType() != null) task.setTaskType(dto.getTaskType());
        if (dto.getStatus() != null) task.setStatus(dto.getStatus());
        if (dto.getPriority() != null) task.setPriority(dto.getPriority());
        if (dto.getDueDate() != null) task.setDueDate(parseDate(dto.getDueDate()));
        if (dto.getDueTime() != null) task.setDueTime(parseTime(dto.getDueTime()));
        if (dto.getAssignedTo() != null) task.setAssignedTo(dto.getAssignedTo());
        if (dto.getAssignedBy() != null) task.setAssignedBy(dto.getAssignedBy());
        if (dto.getPatientId() != null) task.setPatientId(dto.getPatientId());
        if (dto.getPatientName() != null) task.setPatientName(dto.getPatientName());
        if (dto.getEncounterId() != null) task.setEncounterId(dto.getEncounterId());
        if (dto.getReferenceType() != null) task.setReferenceType(dto.getReferenceType());
        if (dto.getReferenceId() != null) task.setReferenceId(dto.getReferenceId());
        if (dto.getNotes() != null) task.setNotes(dto.getNotes());

        return toDto(repo.save(task));
    }

    @Transactional
    public void delete(Long id) {
        var task = repo.findById(id)
                .filter(t -> t.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Task not found: " + id));
        repo.delete(task);
    }

    @Transactional(readOnly = true)
    public Page<ClinicalTaskDto> getAll(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return repo.findByOrgAlias(orgAlias(), pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ClinicalTaskDto> listFiltered(int page, int size, String q, String status,
                                               String priority, String taskType) {
        // Use unsorted pageable — native query has its own ORDER BY
        var pageable = PageRequest.of(page, size);
        String org = orgAlias();
        String searchQ = (q != null && !q.isBlank()) ? q.trim() : null;
        boolean isOverdue = "overdue".equalsIgnoreCase(status);
        String statusFilter = (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status) && !isOverdue)
                ? status : null;
        String priorityFilter = (priority != null && !priority.isBlank() && !"all".equalsIgnoreCase(priority))
                ? priority : null;
        String typeFilter = (taskType != null && !taskType.isBlank() && !"all".equalsIgnoreCase(taskType))
                ? taskType : null;
        return repo.findFiltered(org, searchQ, statusFilter, priorityFilter, typeFilter,
                isOverdue, LocalDate.now(), pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<ClinicalTaskDto> getByStatus(String status) {
        return repo.findByOrgAliasAndStatusOrderByDueDateAsc(orgAlias(), status)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ClinicalTaskDto> getOverdue() {
        return repo.findOverdue(orgAlias(), LocalDate.now())
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ClinicalTaskDto> getByAssignee(String assignee) {
        return repo.findByOrgAliasAndAssignedToOrderByDueDateAsc(orgAlias(), assignee)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ClinicalTaskDto> getByPatient(Long patientId) {
        return repo.findByOrgAliasAndPatientIdOrderByCreatedAtDesc(orgAlias(), patientId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ClinicalTaskDto> search(String query) {
        if (query == null || query.isBlank()) {
            return repo.findByOrgAlias(orgAlias(), PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "createdAt")))
                    .stream().map(this::toDto).toList();
        }
        return repo.search(orgAlias(), query.trim())
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public ClinicalTaskDto complete(Long id, String completedBy) {
        var task = repo.findById(id)
                .filter(t -> t.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Task not found: " + id));
        task.setStatus("completed");
        task.setCompletedAt(LocalDateTime.now());
        task.setCompletedBy(completedBy);
        return toDto(repo.save(task));
    }

    @Transactional(readOnly = true)
    public Map<String, Long> dashboardStats() {
        String org = orgAlias();
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("pending", repo.countByOrgAliasAndStatus(org, "pending"));
        stats.put("inProgress", repo.countByOrgAliasAndStatus(org, "in_progress"));
        stats.put("completed", repo.countByOrgAliasAndStatus(org, "completed"));
        stats.put("overdue", repo.countOverdue(org, LocalDate.now()));
        return stats;
    }

    // ── Date/time parsing (flexible, matching LabOrderService patterns) ──────

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            // Handle ISO instant like "2026-02-22T03:03:59.059Z"
            if (s.contains("T")) return Instant.parse(s).atZone(ZoneId.systemDefault()).toLocalDate();
            // Handle DD-MM-YYYY
            if (s.matches("\\d{2}-\\d{2}-\\d{4}")) return LocalDate.parse(s, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            // Handle YYYY-MM-DD
            return LocalDate.parse(s);
        } catch (Exception e) {
            log.warn("Failed to parse date '{}', returning null", s);
            return null;
        }
    }

    private LocalTime parseTime(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalTime.parse(s);
        } catch (Exception e) {
            log.warn("Failed to parse time '{}', returning null", s);
            return null;
        }
    }

    private LocalDateTime parseDateTime(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            if (s.endsWith("Z") || s.contains("+")) return Instant.parse(s).atZone(ZoneId.systemDefault()).toLocalDateTime();
            return LocalDateTime.parse(s);
        } catch (Exception e) {
            log.warn("Failed to parse datetime '{}', returning null", s);
            return null;
        }
    }

    // ── Entity → DTO mapping ────────────────────────────────────────────────

    private ClinicalTaskDto toDto(ClinicalTask e) {
        return ClinicalTaskDto.builder()
                .id(e.getId())
                .title(e.getTitle())
                .description(e.getDescription())
                .taskType(e.getTaskType())
                .status(e.getStatus())
                .priority(e.getPriority())
                .dueDate(e.getDueDate() != null ? e.getDueDate().toString() : null)
                .dueTime(e.getDueTime() != null ? e.getDueTime().toString() : null)
                .assignedTo(e.getAssignedTo())
                .assignedBy(e.getAssignedBy())
                .patientId(e.getPatientId())
                .patientName(e.getPatientName())
                .encounterId(e.getEncounterId())
                .referenceType(e.getReferenceType())
                .referenceId(e.getReferenceId())
                .completedAt(e.getCompletedAt() != null ? e.getCompletedAt().toString() : null)
                .completedBy(e.getCompletedBy())
                .notes(e.getNotes())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }
}
