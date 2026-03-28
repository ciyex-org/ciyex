package org.ciyex.ehr.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.inventory.dto.InvMaintenanceDto;
import org.ciyex.ehr.inventory.entity.MaintenanceTask;
import org.ciyex.ehr.inventory.repository.MaintenanceTaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvMaintenanceService2 {

    private final MaintenanceTaskRepository repo;

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    @Transactional
    public InvMaintenanceDto create(InvMaintenanceDto dto) {
        var entity = MaintenanceTask.builder()
                .equipmentName(dto.getEquipmentName())
                .equipmentId(dto.getEquipmentId())
                .category(dto.getCategory())
                .location(dto.getLocation())
                .dueDate(dto.getDueDate() != null && !dto.getDueDate().isBlank() ? LocalDate.parse(dto.getDueDate()) : null)
                .lastServiceDate(dto.getLastServiceDate() != null && !dto.getLastServiceDate().isBlank() ? LocalDate.parse(dto.getLastServiceDate()) : null)
                .nextServiceDate(dto.getNextServiceDate() != null && !dto.getNextServiceDate().isBlank() ? LocalDate.parse(dto.getNextServiceDate()) : null)
                .assignee(dto.getAssignee())
                .vendor(dto.getVendor())
                .priority(dto.getPriority())
                .status(dto.getStatus() != null ? dto.getStatus() : "scheduled")
                .notes(dto.getNotes())
                .cost(dto.getCost())
                .orgAlias(orgAlias())
                .build();
        return toDto(repo.save(entity));
    }

    @Transactional(readOnly = true)
    public InvMaintenanceDto getById(Long id) {
        return repo.findById(id)
                .filter(m -> m.getOrgAlias().equals(orgAlias()))
                .map(this::toDto)
                .orElseThrow(() -> new NoSuchElementException("Maintenance task not found: " + id));
    }

    @Transactional
    public InvMaintenanceDto update(Long id, InvMaintenanceDto dto) {
        var entity = repo.findById(id)
                .filter(m -> m.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Maintenance task not found: " + id));

        entity.setEquipmentName(dto.getEquipmentName());
        entity.setEquipmentId(dto.getEquipmentId());
        entity.setCategory(dto.getCategory());
        entity.setLocation(dto.getLocation());
        entity.setDueDate(dto.getDueDate() != null && !dto.getDueDate().isBlank() ? LocalDate.parse(dto.getDueDate()) : null);
        entity.setLastServiceDate(dto.getLastServiceDate() != null && !dto.getLastServiceDate().isBlank() ? LocalDate.parse(dto.getLastServiceDate()) : null);
        entity.setNextServiceDate(dto.getNextServiceDate() != null && !dto.getNextServiceDate().isBlank() ? LocalDate.parse(dto.getNextServiceDate()) : null);
        entity.setAssignee(dto.getAssignee());
        entity.setVendor(dto.getVendor());
        entity.setPriority(dto.getPriority());
        entity.setStatus(dto.getStatus());
        entity.setNotes(dto.getNotes());
        entity.setCost(dto.getCost());

        return toDto(repo.save(entity));
    }

    @Transactional
    public InvMaintenanceDto updateStatus(Long id, String status) {
        var entity = repo.findById(id)
                .filter(m -> m.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Maintenance task not found: " + id));
        entity.setStatus(status);
        return toDto(repo.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        var entity = repo.findById(id)
                .filter(m -> m.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Maintenance task not found: " + id));
        repo.delete(entity);
    }

    @Transactional(readOnly = true)
    public Page<InvMaintenanceDto> getAll(Pageable pageable) {
        return repo.findByOrgAlias(orgAlias(), pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public long countOverdue() {
        return repo.countByOrgAliasAndStatusAndDueDateBefore(orgAlias(), "scheduled", LocalDate.now());
    }

    private InvMaintenanceDto toDto(MaintenanceTask e) {
        return InvMaintenanceDto.builder()
                .id(e.getId())
                .equipmentName(e.getEquipmentName())
                .equipmentId(e.getEquipmentId())
                .category(e.getCategory())
                .location(e.getLocation())
                .dueDate(e.getDueDate() != null ? e.getDueDate().toString() : null)
                .lastServiceDate(e.getLastServiceDate() != null ? e.getLastServiceDate().toString() : null)
                .nextServiceDate(e.getNextServiceDate() != null ? e.getNextServiceDate().toString() : null)
                .assignee(e.getAssignee())
                .vendor(e.getVendor())
                .priority(e.getPriority())
                .status(e.getStatus())
                .notes(e.getNotes())
                .cost(e.getCost())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }
}
