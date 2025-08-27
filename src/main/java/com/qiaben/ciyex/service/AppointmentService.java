package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.AppointmentDTO;
import com.qiaben.ciyex.entity.Appointment;
import com.qiaben.ciyex.repository.AppointmentRepository;
import com.qiaben.ciyex.storage.ExternalAppointmentStorage;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import com.qiaben.ciyex.dto.integration.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class AppointmentService {

    private final AppointmentRepository repository;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;

    @Autowired
    public AppointmentService(AppointmentRepository repository,
                              ExternalStorageResolver storageResolver,
                              OrgIntegrationConfigProvider configProvider) {
        this.repository = repository;
        this.storageResolver = storageResolver;
        this.configProvider = configProvider;
    }

    // -------- Create --------
    @Transactional
    public AppointmentDTO create(AppointmentDTO dto) {
        Long orgId = getCurrentOrgId();
        if (orgId == null) {
            throw new SecurityException("No orgId available in request context");
        }

        Appointment entity = mapToEntity(dto);
        entity.setOrgId(orgId); // ✅ enforce orgId
        entity.setCreatedDate(LocalDateTime.now().toString());
        entity.setLastModifiedDate(LocalDateTime.now().toString());

        // Sync to external storage if configured
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            try {
                ExternalAppointmentStorage externalStorage =
                        (ExternalAppointmentStorage) storageResolver.resolve(AppointmentDTO.class);
                String externalId = externalStorage.create(dto);
                log.info("Synced appointment to external storage with externalId {} for orgId {}", externalId, orgId);
            } catch (Exception e) {
                log.error("Failed to sync appointment to external storage for orgId {}, {}", orgId, e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e);
            }
        }

        entity = repository.save(entity);
        return mapToDto(entity);
    }

    // -------- Retrieve --------
    @Transactional(readOnly = true)
    public AppointmentDTO getById(Long id) {
        Long orgId = getCurrentOrgId();
        Appointment entity = repository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new RuntimeException("Appointment not found with id: " + id + " for org " + orgId));
        return mapToDto(entity);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentDTO> getAll(Pageable pageable) {
        Long orgId = getCurrentOrgId();
        return repository.findAllByOrgId(orgId, pageable).map(this::mapToDto);
    }

    // -------- Update --------
    @Transactional
    public AppointmentDTO update(Long id, AppointmentDTO dto) {
        Long orgId = getCurrentOrgId();
        Appointment entity = repository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new RuntimeException("Appointment not found with id: " + id + " for org " + orgId));

        updateEntityFromDto(entity, dto);
        entity.setOrgId(orgId); // ✅ keep orgId
        entity.setLastModifiedDate(LocalDateTime.now().toString());

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            try {
                ExternalAppointmentStorage externalStorage =
                        (ExternalAppointmentStorage) storageResolver.resolve(AppointmentDTO.class);
                externalStorage.update(dto, String.valueOf(entity.getId()));
                log.info("Updated appointment {} in external storage for org {}", entity.getId(), orgId);
            } catch (Exception e) {
                log.error("Failed to update appointment in external storage: {}", e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e);
            }
        }

        entity = repository.save(entity);
        return mapToDto(entity);
    }

    // -------- Delete --------
    @Transactional
    public void delete(Long id) {
        Long orgId = getCurrentOrgId();
        Appointment entity = repository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new RuntimeException("Appointment not found with id: " + id + " for org " + orgId));

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            try {
                ExternalAppointmentStorage externalStorage =
                        (ExternalAppointmentStorage) storageResolver.resolve(AppointmentDTO.class);
                externalStorage.delete(String.valueOf(entity.getId()));
                log.info("Deleted appointment {} from external storage for org {}", entity.getId(), orgId);
            } catch (Exception e) {
                log.error("Failed to delete appointment from external storage: {}", e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e);
            }
        }

        repository.delete(entity);
    }

    // -------- Mapping Helpers --------
    private Appointment mapToEntity(AppointmentDTO dto) {
        Appointment entity = new Appointment();
        entity.setVisitType(dto.getVisitType());
        entity.setPatientId(dto.getPatientId());
        entity.setProviderId(dto.getProviderId());
        entity.setAppointmentStartDate(dto.getAppointmentStartDate());
        entity.setAppointmentEndDate(dto.getAppointmentEndDate());
        entity.setAppointmentStartTime(dto.getAppointmentStartTime());
        entity.setAppointmentEndTime(dto.getAppointmentEndTime());
        entity.setPriority(dto.getPriority());
        entity.setLocationId(dto.getLocationId()); // ✅ updated
        entity.setStatus(dto.getStatus());
        entity.setReason(dto.getReason());
        return entity;
    }

    private AppointmentDTO mapToDto(Appointment entity) {
        AppointmentDTO dto = new AppointmentDTO();
        dto.setId(entity.getId());
        dto.setOrgId(entity.getOrgId());
        dto.setVisitType(entity.getVisitType());
        dto.setPatientId(entity.getPatientId());
        dto.setProviderId(entity.getProviderId());
        dto.setAppointmentStartDate(entity.getAppointmentStartDate());
        dto.setAppointmentEndDate(entity.getAppointmentEndDate());
        dto.setAppointmentStartTime(entity.getAppointmentStartTime());
        dto.setAppointmentEndTime(entity.getAppointmentEndTime());
        dto.setPriority(entity.getPriority());
        dto.setLocationId(entity.getLocationId()); // ✅ updated
        dto.setStatus(entity.getStatus());
        dto.setReason(entity.getReason());

        AppointmentDTO.Audit audit = new AppointmentDTO.Audit();
        audit.setCreatedDate(entity.getCreatedDate());
        audit.setLastModifiedDate(entity.getLastModifiedDate());
        dto.setAudit(audit);

        return dto;
    }

    @Transactional(readOnly = true)
    public Page<AppointmentDTO> getByPatientId(Long patientId, Pageable pageable) {
        Long orgId = getCurrentOrgId();
        return repository.findAllByPatientIdAndOrgId(patientId, orgId, pageable)
                .map(this::mapToDto);
    }


    private void updateEntityFromDto(Appointment entity, AppointmentDTO dto) {
        if (dto.getVisitType() != null) entity.setVisitType(dto.getVisitType());
        if (dto.getPatientId() != null) entity.setPatientId(dto.getPatientId());
        if (dto.getProviderId() != null) entity.setProviderId(dto.getProviderId());
        if (dto.getAppointmentStartDate() != null) entity.setAppointmentStartDate(dto.getAppointmentStartDate());
        if (dto.getAppointmentEndDate() != null) entity.setAppointmentEndDate(dto.getAppointmentEndDate());
        if (dto.getAppointmentStartTime() != null) entity.setAppointmentStartTime(dto.getAppointmentStartTime());
        if (dto.getAppointmentEndTime() != null) entity.setAppointmentEndTime(dto.getAppointmentEndTime());
        if (dto.getPriority() != null) entity.setPriority(dto.getPriority());
        if (dto.getLocationId() != null) entity.setLocationId(dto.getLocationId()); // ✅ updated
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        if (dto.getReason() != null) entity.setReason(dto.getReason());
    }

    private Long getCurrentOrgId() {
        return RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
    }
}
