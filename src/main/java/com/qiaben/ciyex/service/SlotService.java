package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.SlotDto;
import com.qiaben.ciyex.entity.Slot;
import com.qiaben.ciyex.repository.SlotRepository;
import com.qiaben.ciyex.storage.ExternalSlotStorage;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SlotService {

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
        Long orgId = getCurrentOrgIdOrThrow();
        return repository.countByOrgId(orgId);
    }

    @Transactional
    public SlotDto create(SlotDto dto) {
        Long orgId = getCurrentOrgIdOrThrow();
        dto.setOrgId(orgId);

        String externalId = null;
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            ExternalSlotStorage external =
                    (ExternalSlotStorage) storageResolver.resolve(SlotDto.class);
            externalId = external.createSlot(dto);
        }

        Slot entity = Slot.builder()
                .orgId(orgId)
                .providerId(dto.getProviderId())
                .externalId(externalId)
                .createdDate(LocalDateTime.now().toString())
                .lastModifiedDate(LocalDateTime.now().toString())
                .build();
        entity = repository.save(entity);

        return mergeLocalAndExternal(entity, fetchExternal(externalId));
    }

    @Transactional(readOnly = true)
    public SlotDto getById(Long id) {
        Long orgId = getCurrentOrgIdOrThrow();
        Slot entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Slot not found: " + id));
        if (!orgId.equals(entity.getOrgId())) {
            throw new SecurityException("Access denied");
        }
        return mergeLocalAndExternal(entity, fetchExternal(entity.getExternalId()));
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<SlotDto>> getAllSlots() {
        Long orgId = getCurrentOrgIdOrThrow();
        List<Slot> entities = repository.findAllByOrgId(orgId);
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
        Long orgId = getCurrentOrgIdOrThrow();
        Slot entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Slot not found: " + id));
        if (!orgId.equals(entity.getOrgId())) {
            throw new SecurityException("Access denied");
        }

        dto.setOrgId(orgId);
        dto.setExternalId(entity.getExternalId());
        ExternalSlotStorage external =
                (ExternalSlotStorage) storageResolver.resolve(SlotDto.class);
        external.updateSlot(dto, entity.getExternalId());

        entity.setLastModifiedDate(LocalDateTime.now().toString());
        repository.save(entity);

        return mergeLocalAndExternal(entity, fetchExternal(entity.getExternalId()));
    }

    @Transactional
    public void delete(Long id) {
        Long orgId = getCurrentOrgIdOrThrow();
        Slot entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Slot not found: " + id));
        if (!orgId.equals(entity.getOrgId())) {
            throw new SecurityException("Access denied");
        }

        if (entity.getExternalId() != null) {
            ExternalSlotStorage external =
                    (ExternalSlotStorage) storageResolver.resolve(SlotDto.class);
            external.deleteSlot(entity.getExternalId());
        }
        repository.delete(entity);
    }

    private SlotDto mergeLocalAndExternal(Slot entity, SlotDto externalDto) {
        SlotDto dto = new SlotDto();
        dto.setId(entity.getId());
        dto.setOrgId(entity.getOrgId());
        dto.setProviderId(entity.getProviderId());
        dto.setExternalId(entity.getExternalId());
        SlotDto.Audit audit = new SlotDto.Audit();
        audit.setCreatedDate(entity.getCreatedDate());
        audit.setLastModifiedDate(entity.getLastModifiedDate());
        dto.setAudit(audit);

        if (externalDto != null) {
            dto.setStart(externalDto.getStart());
            dto.setEnd(externalDto.getEnd());
            dto.setStatus(externalDto.getStatus());
            dto.setComment(externalDto.getComment());
        }
        return dto;
    }

    private SlotDto fetchExternal(String externalId) {
        if (externalId == null) return null;
        ExternalSlotStorage external =
                (ExternalSlotStorage) storageResolver.resolve(SlotDto.class);
        return external.getSlot(externalId);
    }

    private Long getCurrentOrgIdOrThrow() {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        if (orgId == null) throw new SecurityException("No orgId in request");
        return orgId;
    }
}
