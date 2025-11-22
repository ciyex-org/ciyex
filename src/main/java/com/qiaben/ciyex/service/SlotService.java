package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.SlotDto;
import com.qiaben.ciyex.entity.Slot;
import com.qiaben.ciyex.repository.SlotRepository;
import com.qiaben.ciyex.storage.ExternalSlotStorage;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SlotService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final SlotRepository repository;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;

    public SlotService(SlotRepository repository,
                       ExternalStorageResolver storageResolver,
                       OrgIntegrationConfigProvider configProvider) {
        this.repository = repository;
        this.storageResolver = storageResolver;
        this.configProvider = configProvider;
    }

    @Transactional(readOnly = true)
    public long countSlotsForCurrentOrg() {

        return repository.count();
    }

    @Transactional
    public SlotDto create(SlotDto dto) {
        // Validate mandatory fields
        validateMandatoryFields(dto);

        String externalId = null;
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            try {
                ExternalSlotStorage external =
                        (ExternalSlotStorage) storageResolver.resolve(SlotDto.class);
                externalId = external.createSlot(dto);
            } catch (Exception e) {
                log.warn("Failed to create in external storage: {}", e.getMessage());
            }
        }

        Slot entity = Slot.builder()
                .providerId(dto.getProviderId())
                .externalId(externalId)
                .start(dto.getStart())
                .end(dto.getEnd())
                .status(dto.getStatus())
                .comment(dto.getComment())
                .build();
        entity = repository.save(entity);

        return mergeLocalAndExternal(entity, fetchExternal(externalId));
    }

    private void validateMandatoryFields(SlotDto dto) {
        StringBuilder errors = new StringBuilder();

        if (dto.getProviderId() == null) {
            errors.append("providerId, ");
        }

        if (errors.length() > 0) {
            // Remove trailing comma and space
            String missingFields = errors.substring(0, errors.length() - 2);
            throw new IllegalArgumentException("Missing mandatory fields: " + missingFields);
        }
    }

    @Transactional(readOnly = true)
    public SlotDto getById(Long id) {

        Slot entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Slot not found: " + id));

        return mergeLocalAndExternal(entity, fetchExternal(entity.getExternalId()));
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<SlotDto>> getAllSlots() {
        List<Slot> entities = repository.findAll();
        List<SlotDto> out = new ArrayList<>();
        for (Slot s : entities) {
            out.add(mergeLocalAndExternal(s, fetchExternal(s.getExternalId())));
        }
        return ApiResponse.<List<SlotDto>>builder()
                .success(true)
                .message("Slots retrieved successfully")
                .data(out)
                .build();
    }

    @Transactional
    public SlotDto update(Long id, SlotDto dto) {

        Slot entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Slot not found: " + id));

        // Update fields if provided
        if (dto.getProviderId() != null) {
            entity.setProviderId(dto.getProviderId());
        }
        if (dto.getStart() != null) {
            entity.setStart(dto.getStart());
        }
        if (dto.getEnd() != null) {
            entity.setEnd(dto.getEnd());
        }
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        if (dto.getComment() != null) {
            entity.setComment(dto.getComment());
        }

        dto.setExternalId(entity.getExternalId());

        // Only update external storage if configured
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && entity.getExternalId() != null) {
            try {
                ExternalSlotStorage external =
                        (ExternalSlotStorage) storageResolver.resolve(SlotDto.class);
                external.updateSlot(dto, entity.getExternalId());
            } catch (Exception e) {
                log.warn("Failed to update external storage: {}", e.getMessage());
            }
        }

        repository.save(entity);

        return mergeLocalAndExternal(entity, fetchExternal(entity.getExternalId()));
    }

    @Transactional
    public void delete(Long id) {

        Slot entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Slot not found: " + id));


        if (entity.getExternalId() != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null) {
                try {
                    ExternalSlotStorage external =
                            (ExternalSlotStorage) storageResolver.resolve(SlotDto.class);
                    external.deleteSlot(entity.getExternalId());
                } catch (Exception e) {
                    log.warn("Failed to delete from external storage: {}", e.getMessage());
                }
            }
        }
        repository.delete(entity);
    }

    private SlotDto mergeLocalAndExternal(Slot entity, SlotDto externalDto) {
        SlotDto dto = new SlotDto();
        dto.setId(entity.getId());
        dto.setProviderId(entity.getProviderId());
        dto.setExternalId(entity.getExternalId());

        // Use local data first
        dto.setStart(entity.getStart());
        dto.setEnd(entity.getEnd());
        dto.setStatus(entity.getStatus());
        dto.setComment(entity.getComment());

        SlotDto.Audit audit = new SlotDto.Audit();
        if (entity.getCreatedDate() != null) {
            audit.setCreatedDate(entity.getCreatedDate().format(DATE_FORMATTER));
        }
        if (entity.getLastModifiedDate() != null) {
            audit.setLastModifiedDate(entity.getLastModifiedDate().format(DATE_FORMATTER));
        }
        dto.setAudit(audit);

        // Override with external data if available
        if (externalDto != null) {
            if (externalDto.getStart() != null) dto.setStart(externalDto.getStart());
            if (externalDto.getEnd() != null) dto.setEnd(externalDto.getEnd());
            if (externalDto.getStatus() != null) dto.setStatus(externalDto.getStatus());
            if (externalDto.getComment() != null) dto.setComment(externalDto.getComment());
        }
        return dto;
    }

    private SlotDto fetchExternal(String externalId) {
        if (externalId == null) return null;
        ExternalSlotStorage external =
                (ExternalSlotStorage) storageResolver.resolve(SlotDto.class);
        return external.getSlot(externalId);
    }


    // Removed usage; kept for compatibility with potential external callers.

}
