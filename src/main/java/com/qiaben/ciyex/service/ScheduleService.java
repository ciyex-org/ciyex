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

        return repository.count();
    }

    @Transactional
    public ScheduleDto create(ScheduleDto dto) {

        if (dto.getProviderId() == null) {
            throw new IllegalArgumentException("providerId is required");
        }
        validateScheduleDto(dto);


// Create in external storage and capture externalId
        String externalId = dto.getExternalId(); // Start with DTO's externalId
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            try {
                ExternalScheduleStorage external =
                        (ExternalScheduleStorage) storageResolver.resolve(ScheduleDto.class);
                String extId = external.createSchedule(dto);
                if (extId != null) {
                    externalId = extId; // Override with external storage ID if available
                    log.info("Successfully created schedule in external storage with externalId: {}", externalId);
                }
            } catch (Exception e) {
                log.warn("Failed to sync with external storage, falling back to local generation: {}", e.getMessage());
                // Fall back to auto-generation if external storage fails
                externalId = null;
            }
        }

        // Auto-generate externalId if not provided, no external storage, or external storage failed
        if (externalId == null) {
            externalId = "SCH-" + System.currentTimeMillis();
            log.info("Auto-generated externalId: {}", externalId);
        }


// Persist minimal linkage locally
        Schedule entity = Schedule.builder()
                .providerId(dto.getProviderId())
                .fhirId(externalId)
                .externalId(externalId)
                .start(dto.getStart())
                .end(dto.getEnd())
                .timezone(dto.getTimezone())
                .serviceCategory(dto.getServiceCategory())
                .serviceType(dto.getServiceType())
                .specialty(dto.getSpecialty())
                .status(dto.getStatus())
                .comment(dto.getComment())
                .actorReferences(dto.getActorReferences() != null ? String.join(",", dto.getActorReferences()) : null)
                .build();

        // Map recurrence if present
        if (dto.getRecurrence() != null) {
            ScheduleDto.Recurrence r = dto.getRecurrence();
            entity.setRecurrenceFrequency(r.getFrequency());
            entity.setRecurrenceInterval(r.getInterval());
            entity.setRecurrenceByWeekday(r.getByWeekday() != null ? String.join(",", r.getByWeekday()) : null);
            entity.setRecurrenceStartDate(r.getStartDate());
            entity.setRecurrenceEndDate(r.getEndDate());
            entity.setRecurrenceStartTime(r.getStartTime());
            entity.setRecurrenceEndTime(r.getEndTime());
            entity.setRecurrenceMaxOccurrences(r.getMaxOccurrences());
            entity.setRecurrenceLocationId(r.getLocationId());
        }

        entity = repository.save(entity);


        return mergeLocalAndExternal(entity, fetchExternal(externalId));
    }


    @Transactional(readOnly = true)
    public ScheduleDto getById(Long id) {

        Schedule entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found with id: " + id));
        return mergeLocalAndExternal(entity, fetchExternal(entity.getExternalId()));
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<ScheduleDto>> getAllSchedules() {

        List<Schedule> entities = repository.findAll();

        // collect all externalIds
        List<String> externalIds = entities.stream()
                .map(Schedule::getExternalId)
                .filter(Objects::nonNull)
                .toList();

        // fetch external schedules in bulk
        Map<String, ScheduleDto> externalMap = new HashMap<>();
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && !externalIds.isEmpty()) {
            try {
                ExternalScheduleStorage external =
                        (ExternalScheduleStorage) storageResolver.resolve(ScheduleDto.class);
                List<ScheduleDto> extDtos = external.getSchedulesByIds(externalIds);
                for (ScheduleDto ext : extDtos) {
                    externalMap.put(ext.getExternalId(), ext);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch external schedules in bulk: {}", e.getMessage());
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

        Schedule entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found with id: " + id));


        // allow providerId update locally (all other details live in external)
        if (dto.getProviderId() != null) entity.setProviderId(dto.getProviderId());
        if (dto.getStart() != null) entity.setStart(dto.getStart());
        if (dto.getEnd() != null) entity.setEnd(dto.getEnd());
        if (dto.getTimezone() != null) entity.setTimezone(dto.getTimezone());
        if (dto.getServiceCategory() != null) entity.setServiceCategory(dto.getServiceCategory());
        if (dto.getServiceType() != null) entity.setServiceType(dto.getServiceType());
        if (dto.getSpecialty() != null) entity.setSpecialty(dto.getSpecialty());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        if (dto.getComment() != null) entity.setComment(dto.getComment());
        if (dto.getActorReferences() != null) entity.setActorReferences(String.join(",", dto.getActorReferences()));

        // Update recurrence if present
        if (dto.getRecurrence() != null) {
            ScheduleDto.Recurrence r = dto.getRecurrence();
            entity.setRecurrenceFrequency(r.getFrequency());
            entity.setRecurrenceInterval(r.getInterval());
            entity.setRecurrenceByWeekday(r.getByWeekday() != null ? String.join(",", r.getByWeekday()) : null);
            entity.setRecurrenceStartDate(r.getStartDate());
            entity.setRecurrenceEndDate(r.getEndDate());
            entity.setRecurrenceStartTime(r.getStartTime());
            entity.setRecurrenceEndTime(r.getEndTime());
            entity.setRecurrenceMaxOccurrences(r.getMaxOccurrences());
            entity.setRecurrenceLocationId(r.getLocationId());
        }

        // sync to external
        dto.setExternalId(entity.getExternalId());
        ExternalScheduleStorage external =
                (ExternalScheduleStorage) storageResolver.resolve(ScheduleDto.class);
        external.updateSchedule(dto, entity.getExternalId());
        repository.save(entity);

        // return merged local+external
        return mergeLocalAndExternal(entity, fetchExternal(entity.getExternalId()));
    }

    @Transactional
    public void delete(Long id) {
        Schedule entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found with id: " + id));

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
        dto.setProviderId(entity.getProviderId());
        dto.setFhirId(entity.getFhirId());
        dto.setExternalId(entity.getFhirId()); // externalId is an alias for fhirId

        // Use entity values first, fall back to external
        dto.setStart(entity.getStart() != null ? entity.getStart() : (externalDto != null ? externalDto.getStart() : null));
        dto.setEnd(entity.getEnd() != null ? entity.getEnd() : (externalDto != null ? externalDto.getEnd() : null));
        dto.setTimezone(entity.getTimezone() != null ? entity.getTimezone() : (externalDto != null ? externalDto.getTimezone() : null));
        dto.setServiceCategory(entity.getServiceCategory() != null ? entity.getServiceCategory() : (externalDto != null ? externalDto.getServiceCategory() : null));
        dto.setServiceType(entity.getServiceType() != null ? entity.getServiceType() : (externalDto != null ? externalDto.getServiceType() : null));
        dto.setSpecialty(entity.getSpecialty() != null ? entity.getSpecialty() : (externalDto != null ? externalDto.getSpecialty() : null));
        dto.setStatus(entity.getStatus() != null ? entity.getStatus() : (externalDto != null ? externalDto.getStatus() : null));
        dto.setComment(entity.getComment() != null ? entity.getComment() : (externalDto != null ? externalDto.getComment() : null));

        // Parse actor references from comma-separated string
        if (entity.getActorReferences() != null) {
            dto.setActorReferences(List.of(entity.getActorReferences().split(",")));
        } else if (externalDto != null && externalDto.getActorReferences() != null) {
            dto.setActorReferences(externalDto.getActorReferences());
        }

        // Map recurrence from entity or external
        if (hasRecurrenceData(entity)) {
            ScheduleDto.Recurrence recurrence = new ScheduleDto.Recurrence();
            recurrence.setFrequency(entity.getRecurrenceFrequency());
            recurrence.setInterval(entity.getRecurrenceInterval());
            if (entity.getRecurrenceByWeekday() != null && !entity.getRecurrenceByWeekday().trim().isEmpty()) {
                recurrence.setByWeekday(List.of(entity.getRecurrenceByWeekday().split(",")));
            }
            recurrence.setStartDate(entity.getRecurrenceStartDate());
            recurrence.setEndDate(entity.getRecurrenceEndDate());
            recurrence.setStartTime(entity.getRecurrenceStartTime());
            recurrence.setEndTime(entity.getRecurrenceEndTime());
            recurrence.setMaxOccurrences(entity.getRecurrenceMaxOccurrences());
            recurrence.setLocationId(entity.getRecurrenceLocationId());
            dto.setRecurrence(recurrence);
        } else if (externalDto != null && externalDto.getRecurrence() != null) {
            dto.setRecurrence(externalDto.getRecurrence());
        }

        // Map audit information from entity
        ScheduleDto.Audit audit = new ScheduleDto.Audit();
        if (entity.getCreatedDate() != null) {
            audit.setCreatedDate(entity.getCreatedDate().toString());
        }
        if (entity.getLastModifiedDate() != null) {
            audit.setLastModifiedDate(entity.getLastModifiedDate().toString());
        }
        dto.setAudit(audit);

        return dto;
    }


    private ScheduleDto fetchExternal(String externalId) {
        if (externalId == null) return null;
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType == null) return null;
        try {
            ExternalScheduleStorage external =
                    (ExternalScheduleStorage) storageResolver.resolve(ScheduleDto.class);
            return external.getSchedule(externalId);
        } catch (Exception e) {
            log.warn("Failed to fetch external schedule: {}", e.getMessage());
            return null;
        }
    }



    private boolean hasRecurrenceData(Schedule entity) {
        return entity.getRecurrenceFrequency() != null ||
               entity.getRecurrenceInterval() != null ||
               entity.getRecurrenceByWeekday() != null ||
               entity.getRecurrenceStartDate() != null ||
               entity.getRecurrenceEndDate() != null ||
               entity.getRecurrenceStartTime() != null ||
               entity.getRecurrenceEndTime() != null ||
               entity.getRecurrenceMaxOccurrences() != null ||
               entity.getRecurrenceLocationId() != null;
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