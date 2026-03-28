package org.ciyex.ehr.careplan.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.careplan.dto.CarePlanDto;
import org.ciyex.ehr.careplan.dto.CarePlanGoalDto;
import org.ciyex.ehr.careplan.dto.CarePlanInterventionDto;
import org.ciyex.ehr.careplan.entity.CarePlan;
import org.ciyex.ehr.careplan.entity.CarePlanGoal;
import org.ciyex.ehr.careplan.entity.CarePlanIntervention;
import org.ciyex.ehr.careplan.repository.CarePlanGoalRepository;
import org.ciyex.ehr.careplan.repository.CarePlanInterventionRepository;
import org.ciyex.ehr.careplan.repository.CarePlanRepository;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarePlanService {

    private final CarePlanRepository repo;
    private final CarePlanGoalRepository goalRepo;
    private final CarePlanInterventionRepository interventionRepo;

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    // ── Care Plan CRUD ──

    @Transactional
    public CarePlanDto create(Long patientId, CarePlanDto dto) {
        var plan = CarePlan.builder()
                .patientId(patientId)
                .patientName(dto.getPatientName())
                .title(dto.getTitle())
                .status(dto.getStatus() != null ? dto.getStatus() : "active")
                .category(dto.getCategory())
                .startDate(parseDate(dto.getStartDate()))
                .endDate(parseDate(dto.getEndDate()))
                .authorName(dto.getAuthorName())
                .description(dto.getDescription())
                .notes(dto.getNotes())
                .orgAlias(orgAlias())
                .build();
        plan = repo.save(plan);

        // Nested goals
        if (dto.getGoals() != null) {
            for (var goalDto : dto.getGoals()) {
                var goal = buildGoalEntity(goalDto, plan);
                goal = goalRepo.save(goal);

                // Nested interventions on goal
                if (goalDto.getInterventions() != null) {
                    for (var intDto : goalDto.getInterventions()) {
                        var intervention = buildInterventionEntity(intDto, plan, goal);
                        interventionRepo.save(intervention);
                    }
                }
            }
        }

        // Plan-level interventions (not tied to a specific goal)
        if (dto.getInterventions() != null) {
            for (var intDto : dto.getInterventions()) {
                var intervention = buildInterventionEntity(intDto, plan, null);
                interventionRepo.save(intervention);
            }
        }

        return getById(plan.getId());
    }

    @Transactional(readOnly = true)
    public CarePlanDto getById(Long id) {
        var plan = repo.findById(id)
                .filter(p -> p.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Care plan not found: " + id));
        return toDto(plan);
    }

    @Transactional
    public CarePlanDto update(Long id, CarePlanDto dto) {
        var plan = repo.findById(id)
                .filter(p -> p.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Care plan not found: " + id));

        if (dto.getTitle() != null) plan.setTitle(dto.getTitle());
        if (dto.getStatus() != null) plan.setStatus(dto.getStatus());
        if (dto.getCategory() != null) plan.setCategory(dto.getCategory());
        if (dto.getStartDate() != null) plan.setStartDate(parseDate(dto.getStartDate()));
        if (dto.getEndDate() != null) plan.setEndDate(parseDate(dto.getEndDate()));
        if (dto.getAuthorName() != null) plan.setAuthorName(dto.getAuthorName());
        if (dto.getDescription() != null) plan.setDescription(dto.getDescription());
        if (dto.getNotes() != null) plan.setNotes(dto.getNotes());
        if (dto.getPatientName() != null) plan.setPatientName(dto.getPatientName());

        plan = repo.save(plan);

        // Save new plan-level interventions (not tied to a specific goal)
        if (dto.getInterventions() != null) {
            for (var intDto : dto.getInterventions()) {
                // Only save new interventions (no id)
                if (intDto.getId() == null) {
                    var intervention = buildInterventionEntity(intDto, plan, null);
                    interventionRepo.save(intervention);
                }
            }
        }

        return toDto(plan);
    }

    @Transactional
    public void delete(Long id) {
        var plan = repo.findById(id)
                .filter(p -> p.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Care plan not found: " + id));
        repo.delete(plan);
    }

    @Transactional(readOnly = true)
    public List<CarePlanDto> getByPatient(Long patientId) {
        return repo.findByOrgAliasAndPatientIdOrderByCreatedAtDesc(orgAlias(), patientId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<CarePlanDto> search(String query) {
        if (query == null || query.isBlank()) {
            return repo.findByOrgAliasOrderByCreatedAtDesc(orgAlias())
                    .stream().map(this::toDto).toList();
        }
        return repo.search(orgAlias(), query.trim())
                .stream().map(this::toDto).toList();
    }

    // ── Goal CRUD ──

    @Transactional
    public CarePlanGoalDto addGoal(Long planId, CarePlanGoalDto dto) {
        var plan = repo.findById(planId)
                .filter(p -> p.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Care plan not found: " + planId));

        var goal = buildGoalEntity(dto, plan);
        goal = goalRepo.save(goal);

        // Nested interventions on goal
        if (dto.getInterventions() != null) {
            for (var intDto : dto.getInterventions()) {
                var intervention = buildInterventionEntity(intDto, plan, goal);
                interventionRepo.save(intervention);
            }
            goal = goalRepo.findById(goal.getId()).orElseThrow();
        }

        return toGoalDto(goal);
    }

    @Transactional
    public CarePlanGoalDto updateGoal(Long goalId, CarePlanGoalDto dto) {
        var goal = goalRepo.findById(goalId)
                .orElseThrow(() -> new NoSuchElementException("Goal not found: " + goalId));

        // Verify org ownership via parent care plan
        if (!goal.getCarePlan().getOrgAlias().equals(orgAlias())) {
            throw new NoSuchElementException("Goal not found: " + goalId);
        }

        if (dto.getDescription() != null) goal.setDescription(dto.getDescription());
        if (dto.getTargetDate() != null) goal.setTargetDate(parseDate(dto.getTargetDate()));
        if (dto.getStatus() != null) goal.setStatus(dto.getStatus());
        if (dto.getMeasure() != null) goal.setMeasure(dto.getMeasure());
        if (dto.getCurrentValue() != null) goal.setCurrentValue(dto.getCurrentValue());
        if (dto.getTargetValue() != null) goal.setTargetValue(dto.getTargetValue());
        if (dto.getPriority() != null) goal.setPriority(dto.getPriority());
        if (dto.getNotes() != null) goal.setNotes(dto.getNotes());

        return toGoalDto(goalRepo.save(goal));
    }

    @Transactional
    public void removeGoal(Long goalId) {
        var goal = goalRepo.findById(goalId)
                .orElseThrow(() -> new NoSuchElementException("Goal not found: " + goalId));

        if (!goal.getCarePlan().getOrgAlias().equals(orgAlias())) {
            throw new NoSuchElementException("Goal not found: " + goalId);
        }

        goalRepo.delete(goal);
    }

    // ── Intervention CRUD ──

    @Transactional(readOnly = true)
    public List<CarePlanGoalDto> getGoalsByPlan(Long planId) {
        repo.findById(planId)
                .filter(p -> p.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Care plan not found: " + planId));
        return goalRepo.findByCarePlanIdOrderByCreatedAtDesc(planId)
                .stream().map(this::toGoalDto).toList();
    }

    @Transactional(readOnly = true)
    public List<CarePlanInterventionDto> getInterventionsByPlan(Long planId) {
        repo.findById(planId)
                .filter(p -> p.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Care plan not found: " + planId));
        return interventionRepo.findByCarePlanIdOrderByCreatedAtDesc(planId)
                .stream().map(this::toInterventionDto).toList();
    }

    @Transactional
    public CarePlanInterventionDto addIntervention(Long planId, CarePlanInterventionDto dto) {
        var plan = repo.findById(planId)
                .filter(p -> p.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Care plan not found: " + planId));

        CarePlanGoal goal = null;
        if (dto.getGoalId() != null) {
            goal = goalRepo.findById(dto.getGoalId())
                    .orElseThrow(() -> new NoSuchElementException("Goal not found: " + dto.getGoalId()));
        }

        var intervention = buildInterventionEntity(dto, plan, goal);
        return toInterventionDto(interventionRepo.save(intervention));
    }

    @Transactional
    public CarePlanInterventionDto updateIntervention(Long intId, CarePlanInterventionDto dto) {
        var intervention = interventionRepo.findById(intId)
                .orElseThrow(() -> new NoSuchElementException("Intervention not found: " + intId));

        if (!intervention.getCarePlan().getOrgAlias().equals(orgAlias())) {
            throw new NoSuchElementException("Intervention not found: " + intId);
        }

        if (dto.getDescription() != null) intervention.setDescription(dto.getDescription());
        if (dto.getAssignedTo() != null) intervention.setAssignedTo(dto.getAssignedTo());
        if (dto.getFrequency() != null) intervention.setFrequency(dto.getFrequency());
        if (dto.getStatus() != null) intervention.setStatus(dto.getStatus());
        if (dto.getNotes() != null) intervention.setNotes(dto.getNotes());

        if (dto.getGoalId() != null) {
            var goal = goalRepo.findById(dto.getGoalId())
                    .orElseThrow(() -> new NoSuchElementException("Goal not found: " + dto.getGoalId()));
            intervention.setGoal(goal);
        }

        return toInterventionDto(interventionRepo.save(intervention));
    }

    @Transactional
    public void removeIntervention(Long intId) {
        var intervention = interventionRepo.findById(intId)
                .orElseThrow(() -> new NoSuchElementException("Intervention not found: " + intId));

        if (!intervention.getCarePlan().getOrgAlias().equals(orgAlias())) {
            throw new NoSuchElementException("Intervention not found: " + intId);
        }

        interventionRepo.delete(intervention);
    }

    @Transactional(readOnly = true)
    public Page<CarePlanDto> getAll(Pageable pageable) {
        return repo.findByOrgAliasOrderByCreatedAtDesc(orgAlias(), pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<CarePlanDto> getByStatus(String status, Pageable pageable) {
        return repo.findByOrgAliasAndStatusOrderByCreatedAtDesc(orgAlias(), status, pageable).map(this::toDto);
    }

    // ── Stats ──

    @Transactional(readOnly = true)
    public Map<String, Long> stats(Long patientId) {
        String org = orgAlias();
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("total", repo.countByOrgAliasAndPatientId(org, patientId));
        stats.put("active", repo.countByOrgAliasAndPatientIdAndStatus(org, patientId, "active"));
        stats.put("draft", repo.countByOrgAliasAndPatientIdAndStatus(org, patientId, "draft"));
        stats.put("completed", repo.countByOrgAliasAndPatientIdAndStatus(org, patientId, "completed"));
        stats.put("revoked", repo.countByOrgAliasAndPatientIdAndStatus(org, patientId, "revoked"));
        stats.put("on_hold", repo.countByOrgAliasAndPatientIdAndStatus(org, patientId, "on_hold"));
        return stats;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> orgStats() {
        String org = orgAlias();
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("total", repo.countByOrgAlias(org));
        stats.put("active", repo.countByOrgAliasAndStatus(org, "active"));
        stats.put("draft", repo.countByOrgAliasAndStatus(org, "draft"));
        stats.put("completed", repo.countByOrgAliasAndStatus(org, "completed"));
        stats.put("revoked", repo.countByOrgAliasAndStatus(org, "revoked"));
        stats.put("on_hold", repo.countByOrgAliasAndStatus(org, "on_hold"));
        return stats;
    }

    // ── Entity Builders ──

    private CarePlanGoal buildGoalEntity(CarePlanGoalDto dto, CarePlan plan) {
        return CarePlanGoal.builder()
                .carePlan(plan)
                .description(dto.getDescription())
                .targetDate(parseDate(dto.getTargetDate()))
                .status(dto.getStatus() != null ? dto.getStatus() : "in_progress")
                .measure(dto.getMeasure())
                .currentValue(dto.getCurrentValue())
                .targetValue(dto.getTargetValue())
                .priority(dto.getPriority() != null ? dto.getPriority() : "medium")
                .notes(dto.getNotes())
                .build();
    }

    private CarePlanIntervention buildInterventionEntity(CarePlanInterventionDto dto, CarePlan plan, CarePlanGoal goal) {
        return CarePlanIntervention.builder()
                .carePlan(plan)
                .goal(goal)
                .description(dto.getDescription())
                .assignedTo(dto.getAssignedTo())
                .frequency(dto.getFrequency())
                .status(dto.getStatus() != null ? dto.getStatus() : "active")
                .notes(dto.getNotes())
                .build();
    }

    // ── Date Parsing (same pattern as LabOrderService) ──

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            if (s.contains("T")) return Instant.parse(s).atZone(ZoneId.systemDefault()).toLocalDate();
            if (s.matches("\\d{2}-\\d{2}-\\d{4}")) return LocalDate.parse(s, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            return LocalDate.parse(s);
        } catch (Exception e) {
            log.warn("Failed to parse date '{}', returning null", s);
            return null;
        }
    }

    // ── DTO Mappers ──

    private CarePlanDto toDto(CarePlan e) {
        var goals = e.getGoals() != null
                ? e.getGoals().stream().map(this::toGoalDto).toList()
                : List.<CarePlanGoalDto>of();

        // Collect all interventions for this care plan
        var interventions = interventionRepo.findByCarePlanIdOrderByCreatedAtDesc(e.getId())
                .stream().map(this::toInterventionDto).toList();

        return CarePlanDto.builder()
                .id(e.getId())
                .patientId(e.getPatientId())
                .patientName(e.getPatientName())
                .title(e.getTitle())
                .status(e.getStatus())
                .category(e.getCategory())
                .startDate(e.getStartDate() != null ? e.getStartDate().toString() : null)
                .endDate(e.getEndDate() != null ? e.getEndDate().toString() : null)
                .authorName(e.getAuthorName())
                .description(e.getDescription())
                .notes(e.getNotes())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .goals(goals)
                .interventions(interventions)
                .build();
    }

    private CarePlanGoalDto toGoalDto(CarePlanGoal e) {
        var interventions = e.getInterventions() != null
                ? e.getInterventions().stream().map(this::toInterventionDto).toList()
                : List.<CarePlanInterventionDto>of();

        return CarePlanGoalDto.builder()
                .id(e.getId())
                .carePlanId(e.getCarePlan() != null ? e.getCarePlan().getId() : null)
                .description(e.getDescription())
                .targetDate(e.getTargetDate() != null ? e.getTargetDate().toString() : null)
                .status(e.getStatus())
                .measure(e.getMeasure())
                .currentValue(e.getCurrentValue())
                .targetValue(e.getTargetValue())
                .priority(e.getPriority())
                .notes(e.getNotes())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .interventions(interventions)
                .build();
    }

    private CarePlanInterventionDto toInterventionDto(CarePlanIntervention e) {
        return CarePlanInterventionDto.builder()
                .id(e.getId())
                .carePlanId(e.getCarePlan() != null ? e.getCarePlan().getId() : null)
                .goalId(e.getGoal() != null ? e.getGoal().getId() : null)
                .description(e.getDescription())
                .assignedTo(e.getAssignedTo())
                .frequency(e.getFrequency())
                .status(e.getStatus())
                .notes(e.getNotes())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }
}
