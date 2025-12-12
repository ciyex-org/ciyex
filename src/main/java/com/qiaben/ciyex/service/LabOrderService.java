package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.LabOrderDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.entity.LabOrder;
import com.qiaben.ciyex.repository.LabOrderRepository;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.qiaben.ciyex.dto.ApiResponse;

@Service
@Slf4j
public class LabOrderService {

    private final LabOrderRepository repository;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;

    public LabOrderService(LabOrderRepository repository,
                           ExternalStorageResolver storageResolver,
                           OrgIntegrationConfigProvider configProvider) {
        this.repository = repository;
        this.storageResolver = storageResolver;
        this.configProvider = configProvider;
    }


    // ---- CREATE ----
    @Transactional
    public LabOrderDto create(LabOrderDto dto) {
        validateMandatory(dto);
        LabOrder order = mapToEntity(dto);
        
        // Generate shared ID for both externalId and fhirId
        String sharedId = java.util.UUID.randomUUID().toString();
        order.setExternalId(sharedId);
        order.setFhirId(sharedId);
        
        order.setCreatedDate(nowString());
        order.setLastModifiedDate(nowString());

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            try {
                ExternalStorage<LabOrderDto> es = storageResolver.resolve(LabOrderDto.class);
                String externalId = es.create(dto);
                // Don't override the shared ID we just set
            } catch (Exception e) {
                log.warn("External create skipped. reason={}", rootMessage(e));
            }
        }
        LabOrder saved = repository.save(order);
        return mapToDto(saved);
    }

    // ---- READ ONE ----
    @Transactional(readOnly = true)
    public LabOrderDto getOne(Long id) {
    LabOrder order = repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("LabOrder not found for id: " + id));
    return mapToDto(order);
    }

    
    // ---- READ ALL (org-scoped) ----
    @Transactional(readOnly = true)
    public List<LabOrderDto> getAll() {
    List<LabOrder> orders = repository.findAll();
    return orders.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    
    // Org-scoped search returning ApiResponse (similar to AllergyIntoleranceService.searchAll)
    @Transactional(readOnly = true)
    public ApiResponse<List<LabOrderDto>> searchAll() {
    List<LabOrderDto> data = repository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    return ApiResponse.<List<LabOrderDto>>builder()
        .success(true)
        .message("Lab orders retrieved successfully")
        .data(data)
        .build();
    }


    // ---- UPDATE ----
    @Transactional
    public LabOrderDto update(Long id, LabOrderDto dto) {
        LabOrder order = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("LabOrder not found for id: " + id));
        // enforce non-blank for mandatory fields when provided in update;
        // also ensure existing entity already has required values when not provided.
        if (dto.getOrderNumber() != null) {
            if (isBlank(dto.getOrderNumber())) throw new IllegalArgumentException("orderNumber cannot be blank");
        } else if (isBlank(order.getOrderNumber())) {
            throw new IllegalStateException("Existing lab order missing required orderNumber");
        }

        if (dto.getTestCode() != null) {
            if (isBlank(dto.getTestCode())) throw new IllegalArgumentException("testCode cannot be blank");
        } else if (isBlank(order.getTestCode())) {
            throw new IllegalStateException("Existing lab order missing required testCode");
        }

        if (dto.getPhysicianName() != null) {
            if (isBlank(dto.getPhysicianName())) throw new IllegalArgumentException("physicianName cannot be blank");
        } else if (isBlank(order.getPhysicianName())) {
            throw new IllegalStateException("Existing lab order missing required physicianName");
        }

        if (dto.getOrderingProvider() != null) {
            if (isBlank(dto.getOrderingProvider())) throw new IllegalArgumentException("orderingProvider cannot be blank");
        } else if (isBlank(order.getOrderingProvider())) {
            throw new IllegalStateException("Existing lab order missing required orderingProvider");
        }

        if (dto.getDiagnosisCode() != null) {
            if (isBlank(dto.getDiagnosisCode())) throw new IllegalArgumentException("diagnosisCode cannot be blank");
        } else if (isBlank(order.getDiagnosisCode())) {
            throw new IllegalStateException("Existing lab order missing required diagnosisCode");
        }

        if (dto.getProcedureCode() != null) {
            if (isBlank(dto.getProcedureCode())) throw new IllegalArgumentException("procedureCode cannot be blank");
        } else if (isBlank(order.getProcedureCode())) {
            throw new IllegalStateException("Existing lab order missing required procedureCode");
        }
        updateEntityFromDto(order, dto);
        order.setLastModifiedDate(nowString());
        LabOrder saved = repository.save(order);
        // External sync
        if (order.getExternalId() != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null) {
                ExternalStorage<LabOrderDto> ext = storageResolver.resolve(LabOrderDto.class);
                ext.update(mapToDto(saved), order.getExternalId());
            }
        }
        return mapToDto(saved);
    }

    // ---- DELETE ----
    @Transactional
    public void delete(Long id) {
        LabOrder order = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("LabOrder not found for id: " + id));
        String externalId = order.getExternalId();
        repository.delete(order);
        // External sync
        if (externalId != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null) {
                ExternalStorage<LabOrderDto> ext = storageResolver.resolve(LabOrderDto.class);
                ext.delete(externalId);
            }
        }
    }

    // ---- helpers ----

    private String rootMessage(Throwable t) {
        String last = null;
        while (t != null) {
            if (t.getMessage() != null) last = t.getMessage();
            t = t.getCause();
        }
        return last;
    }

    private String nowString() { return LocalDateTime.now().toString(); }

    private LabOrder mapToEntity(LabOrderDto dto) {
        LabOrder e = new LabOrder();
        e.setPatientId(dto.getPatientId());
        e.setExternalId(dto.getExternalId());
        e.setFhirId(dto.getFhirId());
        e.setPhysicianName(dto.getPhysicianName());
        e.setOrderDateTime(dto.getOrderDateTime());
        e.setOrderName(dto.getOrderName());
        e.setLabName(dto.getLabName());
        e.setOrderNumber(dto.getOrderNumber());
        e.setTestCode(dto.getTestCode());
        e.setTestDisplay(dto.getTestDisplay());
        e.setStatus(dto.getStatus());
        e.setPriority(dto.getPriority());
        e.setOrderDate(dto.getOrderDate());
        e.setSpecimenId(dto.getSpecimenId());
        e.setNotes(dto.getNotes());
        e.setOrderingProvider(dto.getOrderingProvider());
        e.setDiagnosisCode(dto.getDiagnosisCode());
        e.setProcedureCode(dto.getProcedureCode());
        e.setResult(dto.getResult());
        return e;
    }

    private LabOrderDto mapToDto(LabOrder e) {
        // Migration: if fhirId is null, use externalId
        if (e.getFhirId() == null && e.getExternalId() != null) {
            e.setFhirId(e.getExternalId());
            repository.save(e);
        }
        
        LabOrderDto d = new LabOrderDto();
        d.setId(e.getId());
        d.setPatientId(e.getPatientId());
        d.setExternalId(e.getExternalId());
        d.setFhirId(e.getFhirId());
        d.setPhysicianName(e.getPhysicianName());
        d.setOrderDateTime(e.getOrderDateTime());
        d.setOrderName(e.getOrderName());
        d.setLabName(e.getLabName());
        d.setOrderNumber(e.getOrderNumber());
        d.setTestCode(e.getTestCode());
        d.setTestDisplay(e.getTestDisplay());
        d.setStatus(e.getStatus());
        d.setPriority(e.getPriority());
        d.setOrderDate(e.getOrderDate());
        d.setSpecimenId(e.getSpecimenId());
        d.setNotes(e.getNotes());
        d.setOrderingProvider(e.getOrderingProvider());
        d.setDiagnosisCode(e.getDiagnosisCode());
        d.setProcedureCode(e.getProcedureCode());
        d.setResult(e.getResult());
        if (e.getCreatedDate() != null || e.getLastModifiedDate() != null) {
            LabOrderDto.Audit a = new LabOrderDto.Audit();
            a.setCreatedDate(e.getCreatedDate());
            a.setLastModifiedDate(e.getLastModifiedDate());
            d.setAudit(a);
        }
        return d;
    }

    private void updateEntityFromDto(LabOrder e, LabOrderDto d) {
        if (d.getPatientId() != null) e.setPatientId(d.getPatientId());
        if (d.getFhirId() != null) e.setFhirId(d.getFhirId());
        if (d.getPhysicianName() != null) e.setPhysicianName(d.getPhysicianName());
        if (d.getOrderDateTime() != null) e.setOrderDateTime(d.getOrderDateTime());
        if (d.getOrderName() != null) e.setOrderName(d.getOrderName());
        if (d.getLabName() != null) e.setLabName(d.getLabName());
        if (d.getOrderNumber() != null) e.setOrderNumber(d.getOrderNumber()); // mandatory validated earlier
        if (d.getTestCode() != null) e.setTestCode(d.getTestCode());
        if (d.getTestDisplay() != null) e.setTestDisplay(d.getTestDisplay());
        if (d.getStatus() != null) e.setStatus(d.getStatus());
        if (d.getPriority() != null) e.setPriority(d.getPriority());
        if (d.getOrderDate() != null) e.setOrderDate(d.getOrderDate());
        if (d.getSpecimenId() != null) e.setSpecimenId(d.getSpecimenId());
        if (d.getNotes() != null) e.setNotes(d.getNotes());
        if (d.getOrderingProvider() != null) e.setOrderingProvider(d.getOrderingProvider());
        if (d.getDiagnosisCode() != null) e.setDiagnosisCode(d.getDiagnosisCode());
        if (d.getProcedureCode() != null) e.setProcedureCode(d.getProcedureCode());
        if (d.getResult() != null) e.setResult(d.getResult());
    }

    
    // ---- validation helpers ----
    private void validateMandatory(LabOrderDto dto) {
        if (dto == null) throw new IllegalArgumentException("lab order payload is required");
        if (isBlank(dto.getOrderNumber())) throw new IllegalArgumentException("orderNumber is required");
        if (isBlank(dto.getTestCode())) throw new IllegalArgumentException("testCode is required");
        if (isBlank(dto.getPhysicianName())) throw new IllegalArgumentException("physicianName is required");
        if (isBlank(dto.getOrderingProvider())) throw new IllegalArgumentException("orderingProvider is required");
        if (isBlank(dto.getDiagnosisCode())) throw new IllegalArgumentException("diagnosisCode is required");
        if (isBlank(dto.getProcedureCode())) throw new IllegalArgumentException("procedureCode is required");
    }
    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
