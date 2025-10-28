package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.LabOrderDto;
import com.qiaben.ciyex.entity.LabOrder;
import com.qiaben.ciyex.repository.LabOrderRepository;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LabOrderService {

    private final LabOrderRepository repository;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;

    @Autowired
    public LabOrderService(
            LabOrderRepository repository,
            ExternalStorageResolver storageResolver,
            OrgIntegrationConfigProvider configProvider) {
        this.repository = repository;
        this.storageResolver = storageResolver;
        this.configProvider = configProvider;
    }

    @Transactional
    public LabOrderDto create(LabOrderDto dto) {
        // Tenant isolation is now handled at schema level
        // Long chosenOrgId = resolveOrgIdForCreate(dto, allowedOrgIds);
        
        // ensureRequestContextOrg(chosenOrgId);
        //

        if (dto.getTestCode() == null) {
            throw new IllegalArgumentException("testCode is required");
        }

        LabOrder order = mapToEntity(dto);

        String storageType = safeStorageType();
        if (storageType != null) {
            try {
                ExternalStorage<LabOrderDto> es = storageResolver.resolve(LabOrderDto.class);
                es.create(dto); // Create in external system (no externalId stored)
                log.info("External create OK (no externalId stored).");
            } catch (Exception e) {
                log.warn("External create skipped (DB-only). reason={}", rootMessage(e));
            }
        }

        order = repository.save(order);
        return mapToDto(order);
    }

    @Transactional(readOnly = true)
    public LabOrderDto getById(Long id) {
        LabOrder order = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("LabOrder not found with id: " + id));
        return mapToDto(order);
    }

    @Transactional
    public LabOrderDto update(Long id, LabOrderDto dto) {
        LabOrder order = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("LabOrder not found with id: " + id));
        updateEntityFromDto(order, dto);
        order = repository.save(order);
        return mapToDto(order);
    }

    @Transactional
    public void delete(Long id) {
        LabOrder order = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("LabOrder not found with id: " + id));
        repository.delete(order);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<LabOrderDto>> getAll(Collection<Long> allowedOrgIds) {
        
        List<LabOrder> orders = repository.findAll();
        List<LabOrderDto> dtos = orders.stream().map(this::mapToDto).collect(Collectors.toList());
        return ApiResponse.<List<LabOrderDto>>builder()
                .success(true)
                .message("Lab orders retrieved successfully")
                .data(dtos)
                .build();
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<LabOrderDto>> getAllByPatient(Long patientId) {
        
        List<LabOrder> orders = repository.findAllByPatientId(patientId);
        List<LabOrderDto> dtos = orders.stream().map(this::mapToDto).collect(Collectors.toList());
        return ApiResponse.<List<LabOrderDto>>builder()
                .success(true)
                .message("Lab orders retrieved successfully")
                .data(dtos)
                .build();
    }

    @Transactional(readOnly = true)
    public LabOrderDto getByIdForPatient(Long id, Long patientId) {
        LabOrder order = repository.findByIdAndPatientId(id, patientId)
                .orElseThrow(() -> new RuntimeException("LabOrder not found with id: " + id + " for patient: " + patientId));
        return mapToDto(order);
    }

    @Transactional
    public void deleteForPatient(Long id, Long patientId) {
        LabOrder order = repository.findByIdAndPatientId(id, patientId)
                .orElseThrow(() -> new RuntimeException("LabOrder not found with id: " + id + " for patient: " + patientId));
        repository.delete(order);
    }

    // ---- internals ----


    private String safeStorageType() {
        try {
            return configProvider.getStorageTypeForCurrentOrg();
        } catch (Exception e) {
            log.warn("No org integration config; DB-only. reason={}", rootMessage(e));
            return null;
        }
    }

    private String rootMessage(Throwable t) {
        String last = null;
        while (t != null) {
            if (t.getMessage() != null) last = t.getMessage();
            t = t.getCause();
        }
        return last;
    }

    private LabOrder mapToEntity(LabOrderDto dto) {
        LabOrder e = new LabOrder();
        e.setPatientId(dto.getPatientId());
        e.setPatientExternalId(dto.getPatientExternalId());
        e.setMrn(dto.getMrn());
        e.setEncounterId(dto.getEncounterId());
        e.setPhysicianName(dto.getPhysicianName());
        e.setPatientFirstName(dto.getPatientFirstName());
        e.setPatientLastName(dto.getPatientLastName());
        e.setPatientHomePhone(dto.getPatientHomePhone());
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
        e.setIcdId(dto.getIcdId());
        e.setResult(dto.getResult());
        return e;
    }

    private LabOrderDto mapToDto(LabOrder e) {
        LabOrderDto d = new LabOrderDto();
        d.setId(e.getId());
        // d.setOrgId(e.getOrgId()); // Removed - using tenantName now
        d.setPatientId(e.getPatientId());
        d.setPatientExternalId(e.getPatientExternalId());
        d.setMrn(e.getMrn());
        d.setEncounterId(e.getEncounterId());
        d.setPhysicianName(e.getPhysicianName());
        d.setPatientFirstName(e.getPatientFirstName());
        d.setPatientLastName(e.getPatientLastName());
        d.setPatientHomePhone(e.getPatientHomePhone());
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
        d.setIcdId(e.getIcdId());
        d.setResult(e.getResult());

        if (e.getCreatedDate() != null || e.getLastModifiedDate() != null) {
            LabOrderDto.Audit a = new LabOrderDto.Audit();
            d.setAudit(a);
        }
        return d;
    }

    private void updateEntityFromDto(LabOrder e, LabOrderDto d) {
        // if (d.getOrgId() != null) e.setOrgId(d.getOrgId()); // Removed - using tenantName now
        if (d.getPatientId() != null) e.setPatientId(d.getPatientId());
        if (d.getPatientExternalId() != null) e.setPatientExternalId(d.getPatientExternalId());
        if (d.getMrn() != null) e.setMrn(d.getMrn());
        if (d.getEncounterId() != null) e.setEncounterId(d.getEncounterId());
        if (d.getPhysicianName() != null) e.setPhysicianName(d.getPhysicianName());
        if (d.getPatientFirstName() != null) e.setPatientFirstName(d.getPatientFirstName());
        if (d.getPatientLastName() != null) e.setPatientLastName(d.getPatientLastName());
        if (d.getPatientHomePhone() != null) e.setPatientHomePhone(d.getPatientHomePhone());
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
        if (d.getIcdId() != null) e.setIcdId(d.getIcdId());
        if (d.getResult() != null) e.setResult(d.getResult());
    }
}
