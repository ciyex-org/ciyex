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
        if (dto.getTestCode() == null) throw new IllegalArgumentException("testCode is required");
        LabOrder order = mapToEntity(dto);
        order.setCreatedDate(nowString());
        order.setLastModifiedDate(nowString());

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            try {
                ExternalStorage<LabOrderDto> es = storageResolver.resolve(LabOrderDto.class);
                String externalId = es.create(dto);
                order.setExternalId(externalId);
            } catch (Exception e) {
                log.warn("External create skipped. reason={}", rootMessage(e));
            }
        }
        LabOrder saved = repository.save(order);
        LabOrderDto out = mapToDto(saved);
        out.setExternalId(saved.getExternalId());
        return out;
    }

    // ---- READ ONE ----
    @Transactional(readOnly = true)
    public LabOrderDto getOne(Long id) {
    LabOrder order = repository.findById(id)
        .orElseThrow(() -> new RuntimeException("LabOrder not found id=" + id));
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
            .orElseThrow(() -> new RuntimeException("LabOrder not found id=" + id));
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
            .orElseThrow(() -> new RuntimeException("LabOrder not found id=" + id));
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
        LabOrderDto d = new LabOrderDto();
        d.setId(e.getId());
        d.setPatientId(e.getPatientId());
        d.setExternalId(e.getExternalId());
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
        if (d.getPhysicianName() != null) e.setPhysicianName(d.getPhysicianName());
        if (d.getOrderDateTime() != null) e.setOrderDateTime(d.getOrderDateTime());
        if (d.getOrderName() != null) e.setOrderName(d.getOrderName());
        if (d.getLabName() != null) e.setLabName(d.getLabName());
        if (d.getOrderNumber() != null) e.setOrderNumber(d.getOrderNumber());
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
}
