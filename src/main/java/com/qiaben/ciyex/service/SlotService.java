package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.SlotDto;
import com.qiaben.ciyex.dto.SlotScheduleDto;
import com.qiaben.ciyex.entity.Slot;
import com.qiaben.ciyex.repository.SlotRepository;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import com.qiaben.ciyex.dto.integration.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SlotService {

    private final SlotRepository repository;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Autowired
    public SlotService(SlotRepository repository, ExternalStorageResolver storageResolver, OrgIntegrationConfigProvider configProvider) {
        this.repository = repository;
        this.storageResolver = storageResolver;
        this.configProvider = configProvider;
    }

    @Transactional
    public SlotDto create(SlotDto dto) {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            log.error("No orgId found in RequestContext during create");
            throw new SecurityException("No orgId available in request context");
        }
        log.debug("Verifying access for orgId: {} to create new slot", currentOrgId);
        dto.setOrgId(currentOrgId); // Set orgId for the new slot

        if (dto.getProviderId() == null || dto.getStartTime() == null || dto.getEndTime() == null) {
            throw new IllegalArgumentException("Provider ID, start time, and end time are required");
        }

        Slot slot = mapToEntity(dto);
        slot.setOrgId(currentOrgId);
        slot.setCreatedDate(LocalDateTime.now().toString());
        slot.setLastModifiedDate(LocalDateTime.now().toString());
        String externalId = null;

        // Attempt external storage creation first
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            try {
                ExternalStorage<SlotDto> externalStorage = storageResolver.resolve(SlotDto.class);
                externalId = externalStorage.create(dto);
                log.info("Successfully created slot in external storage with externalId: {} for orgId: {}", externalId, currentOrgId);
            } catch (Exception e) {
                log.error("Failed to create slot in external storage for orgId: {}, error: {}", currentOrgId, e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e); // Rollback transaction
            }
        }

        // Save to database only if external storage succeeded or not configured
        slot.setExternalId(externalId);
        slot = repository.save(slot);
        log.info("Created slot with id: {} and externalId: {} in DB for orgId: {}", slot.getId(), externalId, currentOrgId);

        return mapToDto(slot);
    }

    @Transactional(readOnly = true)
    public SlotDto getById(Long id) {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            log.error("No orgId found in RequestContext during getById for id: {}", id);
            throw new SecurityException("No orgId available in request context");
        }
        log.debug("Verifying access for orgId: {} to slot with id: {}", currentOrgId, id);

        Slot slot = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Slot not found with id: " + id));
        if (!currentOrgId.equals(slot.getOrgId())) {
            throw new SecurityException("Access denied: Slot id " + id + " does not belong to orgId " + currentOrgId);
        }
        log.info("Found slot in DB with id: {} for orgId: {}", id, currentOrgId);

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        SlotDto slotDto = mapToDto(slot); // Default to DB data
        if (storageType != null && slot.getExternalId() != null) {
            log.debug("Attempting to fetch detailed data from external storage for slot id: {} with externalId: {} for orgId: {}", id, slot.getExternalId(), currentOrgId);
            ExternalStorage<SlotDto> externalStorage = storageResolver.resolve(SlotDto.class);
            SlotDto syncedDto = externalStorage.get(slot.getExternalId());
            if (syncedDto != null) {
                log.info("Successfully loaded detailed data from external storage for slot id: {} for orgId: {}", id, currentOrgId);
                syncedDto.setId(slot.getId()); // Preserve DB ID
                slotDto = syncedDto;
            } else {
                log.warn("No detailed data found in external storage for slot id: {} with externalId: {} for orgId: {}", id, slot.getExternalId(), currentOrgId);
            }
        }

        return slotDto;
    }

    @Transactional
    public SlotDto update(Long id, SlotDto dto) {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            log.error("No orgId found in RequestContext during update for id: {}", id);
            throw new SecurityException("No orgId available in request context");
        }
        log.debug("Verifying access for orgId: {} to slot with id: {}", currentOrgId, id);

        Slot slot = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Slot not found with id: " + id));
        if (!currentOrgId.equals(slot.getOrgId())) {
            throw new SecurityException("Access denied: Slot id " + id + " does not belong to orgId " + currentOrgId);
        }

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && slot.getExternalId() != null) {
            try {
                ExternalStorage<SlotDto> externalStorage = storageResolver.resolve(SlotDto.class);
                externalStorage.update(dto, slot.getExternalId());
                log.info("Successfully updated slot with id: {} and externalId: {} in external storage for orgId: {}", id, slot.getExternalId(), currentOrgId);
            } catch (Exception e) {
                log.error("Failed to update slot in external storage for orgId: {}, error: {}", currentOrgId, e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e); // Rollback transaction
            }
        }

        updateEntityFromDto(slot, dto);
        slot.setLastModifiedDate(LocalDateTime.now().toString());
        slot = repository.save(slot);
        log.info("Updated slot with id: {} and externalId: {} in DB for orgId: {}", id, slot.getExternalId(), currentOrgId);

        dto.setId(id); // Set database id in DTO
        dto.setExternalId(slot.getExternalId()); // Update externalId in DTO if changed
        return dto;
    }

    @Transactional
    public void delete(Long id) {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            log.error("No orgId found in RequestContext during delete for id: {}", id);
            throw new SecurityException("No orgId available in request context");
        }
        log.debug("Verifying access for orgId: {} to slot with id: {}", currentOrgId, id);

        Slot slot = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Slot not found with id: " + id));
        if (!currentOrgId.equals(slot.getOrgId())) {
            throw new SecurityException("Access denied: Slot id " + id + " does not belong to orgId " + currentOrgId);
        }

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && slot.getExternalId() != null) {
            try {
                ExternalStorage<SlotDto> externalStorage = storageResolver.resolve(SlotDto.class);
                externalStorage.delete(slot.getExternalId());
                log.info("Successfully deleted slot with id: {} and externalId: {} from external storage for orgId: {}", id, slot.getExternalId(), currentOrgId);
            } catch (Exception e) {
                log.error("Failed to delete slot from external storage for orgId: {}, error: {}", currentOrgId, e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e); // Rollback transaction
            }
        }

        repository.delete(slot);
        log.info("Deleted slot with id: {} from DB for orgId: {}", id, currentOrgId);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<SlotDto>> getAllSlots() {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            return ApiResponse.<List<SlotDto>>builder()
                    .success(false)
                    .message("No orgId available in request context")
                    .build();
        }
        log.debug("Verifying access for orgId: {} to retrieve all slots", currentOrgId);

        // Fetch all slots directly from the database
        List<Slot> slots = repository.findAllByOrgId(currentOrgId);
        log.info("Retrieved {} slots from DB for orgId: {}", slots.size(), currentOrgId);
        List<SlotDto> slotDtos = slots.stream().map(this::mapToDto).collect(Collectors.toList());

        return ApiResponse.<List<SlotDto>>builder()
                .success(true)
                .message("Slots retrieved successfully")
                .data(slotDtos)
                .build();
    }

    /**
     * Generates and creates slots for a provider's availability schedule based on the provided schedule.
     *
     * @param scheduleDto The schedule definition including providerId, locationId, startDate, and day schedules
     */
    @Transactional
    public void generateProviderSlots(SlotScheduleDto scheduleDto) {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            throw new SecurityException("No orgId available in request context");
        }
        log.info("Generating slots for providerId: {}, locationId: {}, orgId: {} starting from {}",
                scheduleDto.getProviderId(), scheduleDto.getLocationId(), currentOrgId, scheduleDto.getStartDate());

        if (scheduleDto.getProviderId() == null || scheduleDto.getLocationId() == null || scheduleDto.getStartDate() == null || scheduleDto.getSchedule() == null) {
            throw new IllegalArgumentException("Provider ID, location ID, start date, and schedule are required");
        }

        ZoneId zoneId = ZoneId.of("America/Denver"); // MDT timezone
        LocalDate startDate = scheduleDto.getStartDate();
        List<SlotDto> slots = new ArrayList<>();

        for (int i = 0; i < 7; i++) { // Generate for the next 7 days
            LocalDate date = startDate.plusDays(i);
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            String dayName = dayOfWeek.toString(); // e.g., "MONDAY"

            // Find the schedule for this day
            SlotScheduleDto.DaySchedule daySchedule = scheduleDto.getSchedule().stream()
                    .filter(ds -> ds.getDayOfWeek().equalsIgnoreCase(dayName))
                    .findFirst()
                    .orElse(null);

            if (daySchedule != null) {
                LocalTime startTime = LocalTime.parse(daySchedule.getStartTime());
                LocalTime endTime = LocalTime.parse(daySchedule.getEndTime());
                ZonedDateTime startZoned = date.atTime(startTime).atZone(zoneId);
                ZonedDateTime endZoned = date.atTime(endTime).atZone(zoneId);

                SlotDto slotDto = new SlotDto();
                slotDto.setProviderId(scheduleDto.getProviderId());
                slotDto.setLocationId(scheduleDto.getLocationId());
                slotDto.setOrgId(currentOrgId);
                slotDto.setStartTime(startZoned.format(TIME_FORMATTER));
                slotDto.setEndTime(endZoned.format(TIME_FORMATTER));
                slotDto.setStatus("available");
                slots.add(slotDto);
            }
        }

        // Create each slot
        for (SlotDto slotDto : slots) {
            try {
                create(slotDto);
                log.info("Successfully created slot for providerId: {}, locationId: {}, startTime: {}",
                        scheduleDto.getProviderId(), scheduleDto.getLocationId(), slotDto.getStartTime());
            } catch (Exception e) {
                log.error("Failed to create slot for providerId: {}, locationId: {}, startTime: {}, error: {}",
                        scheduleDto.getProviderId(), scheduleDto.getLocationId(), slotDto.getStartTime(), e.getMessage());
            }
        }
    }

    private Slot mapToEntity(SlotDto dto) {
        return Slot.builder()
                .providerId(dto.getProviderId())
                .locationId(dto.getLocationId())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .status(dto.getStatus())
                .orgId(dto.getOrgId())
                .build();
    }

    private SlotDto mapToDto(Slot slot) {
        SlotDto dto = new SlotDto();
        dto.setId(slot.getId());
        dto.setExternalId(slot.getExternalId());
        dto.setProviderId(slot.getProviderId());
        dto.setLocationId(slot.getLocationId());
        dto.setStartTime(slot.getStartTime());
        dto.setEndTime(slot.getEndTime());
        dto.setStatus(slot.getStatus());
        dto.setOrgId(slot.getOrgId());
        if (slot.getCreatedDate() != null || slot.getLastModifiedDate() != null) {
            SlotDto.Audit audit = new SlotDto.Audit();
            audit.setCreatedDate(slot.getCreatedDate());
            audit.setLastModifiedDate(slot.getLastModifiedDate());
            dto.setAudit(audit);
        }
        return dto;
    }

    private void updateEntityFromDto(Slot slot, SlotDto dto) {
        if (dto.getProviderId() != null) slot.setProviderId(dto.getProviderId());
        if (dto.getLocationId() != null) slot.setLocationId(dto.getLocationId());
        if (dto.getStartTime() != null) slot.setStartTime(dto.getStartTime());
        if (dto.getEndTime() != null) slot.setEndTime(dto.getEndTime());
        if (dto.getStatus() != null) slot.setStatus(dto.getStatus());
        if (dto.getOrgId() != null) slot.setOrgId(dto.getOrgId());
    }

    private Long getCurrentOrgId() {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        if (orgId == null) {
            log.warn("orgId is null in RequestContext");
        }
        return orgId;
    }
}