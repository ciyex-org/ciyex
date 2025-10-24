package com.qiaben.ciyex.service;


import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.ScheduleDto;
import com.qiaben.ciyex.entity.Schedule;
import com.qiaben.ciyex.repository.ScheduleRepository;
import com.qiaben.ciyex.storage.ExternalScheduleStorage;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import com.qiaben.ciyex.dto.integration.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.*;


@Service
@Slf4j
public class ScheduleService {


    private final ScheduleRepository repository;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;


    @Autowired
    public ScheduleService(ScheduleRepository repository,
                           ExternalStorageResolver storageResolver,
                           OrgIntegrationConfigProvider configProvider) {
        this.repository = repository;
        this.storageResolver = storageResolver;
        this.configProvider = configProvider;
    }


    @Transactional(readOnly = true)
    public long countSchedulesForCurrentOrg() {
        Long orgId = getCurrentOrgIdOrThrow();
        return repository.countByOrgId(orgId);
    }

    @Transactional
    public ScheduleDto create(ScheduleDto dto) {
        Long orgId = getCurrentOrgIdOrThrow();
        if (dto.getProviderId() == null) {
            throw new IllegalArgumentException("providerId is required");
        }
        dto.setOrgId(orgId);
        validateScheduleDto(dto);


// Create in external storage and capture externalId
        String externalId = null;
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            ExternalScheduleStorage external =
                    (ExternalScheduleStorage) storageResolver.resolve(ScheduleDto.class);
            externalId = external.createSchedule(dto);

        }


// Persist minimal linkage locally
        Schedule entity = Schedule.builder()
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
    public ScheduleDto getById(Long id) {
        Long orgId = getCurrentOrgIdOrThrow();
        Schedule entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found with id: " + id));
        if (!orgId.equals(entity.getOrgId())) {
            throw new SecurityException("Access denied: Schedule does not belong to current org");
        }
        return mergeLocalAndExternal(entity, fetchExternal(entity.getExternalId()));
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<ScheduleDto>> getAllSchedules() {
        Long orgId = getCurrentOrgIdOrThrow();
        List<Schedule> entities = repository.findAllByOrgId(orgId);

        // collect all externalIds
        List<String> externalIds = entities.stream()
                .map(Schedule::getExternalId)
                .filter(Objects::nonNull)
                .toList();

        // fetch external schedules in bulk
        ExternalScheduleStorage external =
                (ExternalScheduleStorage) storageResolver.resolve(ScheduleDto.class);
        Map<String, ScheduleDto> externalMap = new HashMap<>();
        if (!externalIds.isEmpty()) {
            List<ScheduleDto> extDtos = external.getSchedulesByIds(externalIds);
            for (ScheduleDto ext : extDtos) {
                externalMap.put(ext.getExternalId(), ext);
            }
        }

        // merge local + external
        List<ScheduleDto> out = new ArrayList<>();
        for (Schedule s : entities) {
            ScheduleDto merged = mergeLocalAndExternal(
                    s,
                    externalMap.getOrDefault(s.getExternalId(), fetchExternal(s.getExternalId()))
            );
            if (merged.getStatus() == null) {
                merged.setStatus("active");
            }
            out.add(merged);
        }

        return ApiResponse.<List<ScheduleDto>>builder()
                .success(true)
                .message("Schedules retrieved successfully")
                .data(out)
                .build();
    }



    // imports you likely already have:
// import org.springframework.transaction.annotation.Transactional;
// import com.qiaben.ciyex.dto.ScheduleDto;

    @Transactional
    public ScheduleDto update(Long id, ScheduleDto dto) {
        Long orgId = getCurrentOrgIdOrThrow();
        Schedule entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found with id: " + id));
        if (!orgId.equals(entity.getOrgId())) {
            throw new SecurityException("Access denied: Schedule does not belong to current org");
        }

        // allow providerId update locally (all other details live in external)
        if (dto.getProviderId() != null) entity.setProviderId(dto.getProviderId());

        // sync to external
        dto.setOrgId(orgId);
        dto.setExternalId(entity.getExternalId());
        ExternalScheduleStorage external =
                (ExternalScheduleStorage) storageResolver.resolve(ScheduleDto.class);
        external.updateSchedule(dto, entity.getExternalId());


        entity.setLastModifiedDate(java.time.LocalDateTime.now().toString());
        repository.save(entity);

        // return merged local+external
        return mergeLocalAndExternal(entity, fetchExternal(entity.getExternalId()));
    }

    @Transactional
    public void delete(Long id) {
        Long orgId = getCurrentOrgIdOrThrow();
        Schedule entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found with id: " + id));
        if (!orgId.equals(entity.getOrgId())) {
            throw new SecurityException("Access denied: Schedule does not belong to current org");
        }

        if (entity.getExternalId() != null) {
            ExternalScheduleStorage external =
                    (ExternalScheduleStorage) storageResolver.resolve(ScheduleDto.class);
            external.deleteSchedule(entity.getExternalId());

        }
        repository.delete(entity);
    }



    // -------- helpers --------
    private ScheduleDto mergeLocalAndExternal(Schedule entity, ScheduleDto externalDto) {
        ScheduleDto dto = new ScheduleDto();
        dto.setId(entity.getId());
        dto.setOrgId(entity.getOrgId());
        dto.setProviderId(entity.getProviderId());
        dto.setExternalId(entity.getExternalId());
        if (entity.getCreatedDate() != null || entity.getLastModifiedDate() != null) {
            ScheduleDto.Audit audit = new ScheduleDto.Audit();
            audit.setCreatedDate(entity.getCreatedDate());
            audit.setLastModifiedDate(entity.getLastModifiedDate());
            dto.setAudit(audit);
        }
        if (externalDto != null) {
            dto.setStart(externalDto.getStart());
            dto.setEnd(externalDto.getEnd());
            dto.setActorReferences(externalDto.getActorReferences());
            dto.setServiceCategory(externalDto.getServiceCategory());
            dto.setServiceType(externalDto.getServiceType());
            dto.setSpecialty(externalDto.getSpecialty());
            dto.setStatus(externalDto.getStatus());
            dto.setComment(externalDto.getComment());
            dto.setTimezone(externalDto.getTimezone());
            dto.setRecurrence(externalDto.getRecurrence());
        }
        return dto;
    }


    private ScheduleDto fetchExternal(String externalId) {
        if (externalId == null) return null;
        ExternalScheduleStorage external =
                (ExternalScheduleStorage) storageResolver.resolve(ScheduleDto.class);
        return external.getSchedule(externalId);
    }


    private Long getCurrentOrgIdOrThrow() {
        // Tenant isolation is now handled at schema level
        // orgId is no longer tracked in RequestContext
        return null;
    }
    private void validateScheduleDto(ScheduleDto dto) {
        if (dto.getRecurrence() == null) {
            // One-time schedule
            if (dto.getStart() == null || dto.getEnd() == null || dto.getTimezone() == null) {
                throw new IllegalArgumentException("One-time schedule requires start, end, and timezone");
            }
        } else {
            // Recurring schedule
            ScheduleDto.Recurrence r = dto.getRecurrence();
            if (r.getFrequency() == null || r.getStartDate() == null ||
                    r.getStartTime() == null || r.getEndTime() == null) {
                throw new IllegalArgumentException("Recurring schedule requires frequency, startDate, startTime, and endTime");
            }
            if (r.getEndDate() != null && r.getStartDate().compareTo(r.getEndDate()) > 0) {
                throw new IllegalArgumentException("recurrence.endDate cannot be before startDate");
            }
        }
    }

}