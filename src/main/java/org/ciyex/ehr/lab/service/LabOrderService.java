package org.ciyex.ehr.lab.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.lab.dto.LabOrderDto;
import org.ciyex.ehr.lab.entity.LabOrder;
import org.ciyex.ehr.lab.repository.LabOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabOrderService {

    private final LabOrderRepository repo;

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    @Transactional
    public LabOrderDto create(Long patientId, LabOrderDto dto) {
        var order = LabOrder.builder()
                .patientId(patientId)
                .orderNumber(dto.getOrderNumber())
                .orderName(dto.getOrderName())
                .testCode(dto.getTestCode())
                .testDisplay(dto.getTestDisplay())
                .status(dto.getStatus() != null ? dto.getStatus() : "active")
                .priority(dto.getPriority() != null ? dto.getPriority() : "routine")
                .orderDate(parseDate(dto.getOrderDate()))
                .orderDateTime(parseDateTime(dto.getOrderDateTime()))
                .labName(dto.getLabName())
                .orderingProvider(dto.getOrderingProvider())
                .physicianName(dto.getPhysicianName())
                .specimenId(dto.getSpecimenId())
                .diagnosisCode(dto.getDiagnosisCode())
                .procedureCode(dto.getProcedureCode())
                .resultStatus(dto.getResult() != null ? dto.getResult() : "Pending")
                .notes(dto.getNotes())
                .orgAlias(orgAlias())
                .build();
        order = repo.save(order);
        return toDto(order);
    }

    @Transactional(readOnly = true)
    public List<LabOrderDto> getByPatient(Long patientId) {
        return repo.findByOrgAliasAndPatientIdOrderByOrderDateDesc(orgAlias(), patientId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public LabOrderDto getById(Long patientId, Long orderId) {
        return repo.findById(orderId)
                .filter(o -> o.getOrgAlias().equals(orgAlias()) && o.getPatientId().equals(patientId))
                .map(this::toDto)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
    }

    @Transactional
    public LabOrderDto update(Long patientId, Long orderId, LabOrderDto dto) {
        var order = repo.findById(orderId)
                .filter(o -> o.getOrgAlias().equals(orgAlias()) && o.getPatientId().equals(patientId))
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));

        if (dto.getOrderName() != null) order.setOrderName(dto.getOrderName());
        if (dto.getTestCode() != null) order.setTestCode(dto.getTestCode());
        if (dto.getTestDisplay() != null) order.setTestDisplay(dto.getTestDisplay());
        if (dto.getStatus() != null) order.setStatus(dto.getStatus());
        if (dto.getPriority() != null) order.setPriority(dto.getPriority());
        if (dto.getLabName() != null) order.setLabName(dto.getLabName());
        if (dto.getOrderingProvider() != null) order.setOrderingProvider(dto.getOrderingProvider());
        if (dto.getPhysicianName() != null) order.setPhysicianName(dto.getPhysicianName());
        if (dto.getSpecimenId() != null) order.setSpecimenId(dto.getSpecimenId());
        if (dto.getDiagnosisCode() != null) order.setDiagnosisCode(dto.getDiagnosisCode());
        if (dto.getProcedureCode() != null) order.setProcedureCode(dto.getProcedureCode());
        if (dto.getResult() != null) order.setResultStatus(dto.getResult());
        if (dto.getNotes() != null) order.setNotes(dto.getNotes());

        return toDto(repo.save(order));
    }

    @Transactional
    public void delete(Long patientId, Long orderId) {
        var order = repo.findById(orderId)
                .filter(o -> o.getOrgAlias().equals(orgAlias()) && o.getPatientId().equals(patientId))
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        repo.delete(order);
    }

    @Transactional(readOnly = true)
    public List<LabOrderDto> search(String query) {
        if (query == null || query.isBlank()) {
            return repo.findByOrgAliasOrderByCreatedAtDesc(orgAlias())
                    .stream().map(this::toDto).toList();
        }
        return repo.search(orgAlias(), query.trim())
                .stream().map(this::toDto).toList();
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return LocalDate.now();
        try {
            // Handle ISO instant like "2026-02-22T03:03:59.059Z"
            if (s.contains("T")) return Instant.parse(s).atZone(ZoneId.systemDefault()).toLocalDate();
            // Handle DD-MM-YYYY
            if (s.matches("\\d{2}-\\d{2}-\\d{4}")) return LocalDate.parse(s, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            // Handle YYYY-MM-DD
            return LocalDate.parse(s);
        } catch (Exception e) {
            log.warn("Failed to parse date '{}', using today", s);
            return LocalDate.now();
        }
    }

    private LocalDateTime parseDateTime(String s) {
        if (s == null || s.isBlank()) return LocalDateTime.now();
        try {
            if (s.endsWith("Z") || s.contains("+")) return Instant.parse(s).atZone(ZoneId.systemDefault()).toLocalDateTime();
            return LocalDateTime.parse(s);
        } catch (Exception e) {
            log.warn("Failed to parse datetime '{}', using now", s);
            return LocalDateTime.now();
        }
    }

    private LabOrderDto toDto(LabOrder e) {
        return LabOrderDto.builder()
                .id(e.getId())
                .patientId(e.getPatientId())
                .orderNumber(e.getOrderNumber())
                .orderName(e.getOrderName())
                .testCode(e.getTestCode())
                .testDisplay(e.getTestDisplay())
                .status(e.getStatus())
                .priority(e.getPriority())
                .orderDate(e.getOrderDate() != null ? e.getOrderDate().toString() : null)
                .orderDateTime(e.getOrderDateTime() != null ? e.getOrderDateTime().toString() : null)
                .labName(e.getLabName())
                .orderingProvider(e.getOrderingProvider())
                .physicianName(e.getPhysicianName())
                .specimenId(e.getSpecimenId())
                .diagnosisCode(e.getDiagnosisCode())
                .procedureCode(e.getProcedureCode())
                .result(e.getResultStatus())
                .notes(e.getNotes())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }
}
