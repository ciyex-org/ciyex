package org.ciyex.ehr.service;

import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.SlotDto;
import org.ciyex.ehr.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-only Slot Service.
 * Uses FHIR Slot resource directly via FhirClientService.
 * No local database storage - all data stored in FHIR server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SlotService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public SlotDto create(SlotDto dto) {
        validateMandatoryFields(dto);

        log.debug("Creating FHIR Slot for provider: {}", dto.getProviderId());

        try {
            Slot slot = toFhirSlot(dto);
            var outcome = fhirClientService.create(slot, getPracticeId());
            String fhirId = outcome.getId().getIdPart();

            dto.setId(Long.parseLong(fhirId));
            dto.setFhirId(fhirId);
            dto.setExternalId(fhirId);

            // Set audit information
            SlotDto.Audit audit = new SlotDto.Audit();
            audit.setCreatedDate(java.time.LocalDateTime.now().toString());
            audit.setLastModifiedDate(java.time.LocalDateTime.now().toString());
            dto.setAudit(audit);

            log.info("Created FHIR Slot with id: {}", fhirId);

            return dto;
        } catch (Exception e) {
            log.error("Failed to create slot: {}", e.getMessage());
            throw new RuntimeException("Failed to create slot: " + e.getMessage(), e);
        }
    }

    // GET BY ID (FHIR ID)
    public SlotDto getById(String fhirId) {
        if (fhirId == null || fhirId.trim().isEmpty()) {
            throw new IllegalArgumentException("Slot ID cannot be null or empty");
        }
        
        log.debug("Getting FHIR Slot: {}", fhirId);
        
        try {
            Slot slot = fhirClientService.read(Slot.class, fhirId, getPracticeId());
            if (slot == null) {
                throw new IllegalArgumentException("Slot not found for slotId=" + fhirId);
            }
            return fromFhirSlot(slot);
        } catch (Exception e) {
            log.error("Failed to retrieve slot with ID {}: {}", fhirId, e.getMessage());
            throw new RuntimeException("Failed to retrieve slot: Slot not found for slotId=" + fhirId);
        }
    }

    // GET ALL
    public ApiResponse<List<SlotDto>> getAllSlots() {
        log.debug("Getting all FHIR Slots");

        Bundle bundle = fhirClientService.search(Slot.class, getPracticeId());
        List<Slot> slots = fhirClientService.extractResources(bundle, Slot.class);

        List<SlotDto> dtos = slots.stream()
                .map(this::fromFhirSlot)
                .collect(Collectors.toList());

        return ApiResponse.<List<SlotDto>>builder()
                .success(true)
                .message("Slots retrieved successfully")
                .data(dtos)
                .build();
    }

    // GET BY SCHEDULE
    public List<SlotDto> getByScheduleId(String scheduleId) {
        log.debug("Getting FHIR Slots for schedule: {}", scheduleId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Slot.class)
                .where(new ReferenceClientParam("schedule").hasId("Schedule/" + scheduleId))
                .returnBundle(Bundle.class)
                .execute();

        List<Slot> slots = fhirClientService.extractResources(bundle, Slot.class);
        return slots.stream().map(this::fromFhirSlot).collect(Collectors.toList());
    }

    // UPDATE
    public SlotDto update(String fhirId, SlotDto dto) {
        if (fhirId == null || fhirId.trim().isEmpty()) {
            throw new IllegalArgumentException("Slot ID cannot be null or empty");
        }
        
        validateMandatoryFields(dto);
        
        log.debug("Updating FHIR Slot: {}", fhirId);

        try {
            // Verify slot exists
            fhirClientService.read(Slot.class, fhirId, getPracticeId());
            
            Slot slot = toFhirSlot(dto);
            slot.setId(fhirId);
            fhirClientService.update(slot, getPracticeId());

            dto.setId(Long.parseLong(fhirId));
            dto.setFhirId(fhirId);
            dto.setExternalId(fhirId);
            
            // Set audit information
            SlotDto.Audit audit = new SlotDto.Audit();
            audit.setCreatedDate(dto.getAudit() != null && dto.getAudit().getCreatedDate() != null 
                ? dto.getAudit().getCreatedDate() 
                : java.time.LocalDateTime.now().toString());
            audit.setLastModifiedDate(java.time.LocalDateTime.now().toString());
            dto.setAudit(audit);
            
            return dto;
        } catch (Exception e) {
            log.error("Failed to update slot with ID {}: {}", fhirId, e.getMessage());
            throw new RuntimeException("Failed to update slot: Slot not found for slotId=" + fhirId);
        }
    }

    // DELETE
    public void delete(String fhirId) {
        if (fhirId == null || fhirId.trim().isEmpty()) {
            throw new IllegalArgumentException("Slot ID cannot be null or empty");
        }
        
        log.debug("Deleting FHIR Slot: {}", fhirId);
        
        try {
            // Verify slot exists before deleting
            fhirClientService.read(Slot.class, fhirId, getPracticeId());
            fhirClientService.delete(Slot.class, fhirId, getPracticeId());
            log.info("Successfully deleted slot with ID: {}", fhirId);
        } catch (Exception e) {
            log.error("Failed to delete slot with ID {}: {}", fhirId, e.getMessage());
            throw new RuntimeException("Failed to delete slot: Slot not found for slotId=" + fhirId);
        }
    }

    // -------- FHIR Mapping --------

    private Slot toFhirSlot(SlotDto dto) {
        Slot s = new Slot();

        // Status
        if (dto.getStatus() != null) {
            s.setStatus(mapToSlotStatus(dto.getStatus()));
        } else {
            s.setStatus(Slot.SlotStatus.FREE);
        }

        // Schedule reference (using providerId as schedule reference)
        if (dto.getProviderId() != null) {
            s.setSchedule(new Reference("Schedule/" + dto.getProviderId()));
        }

        // Start/End times
        if (dto.getStart() != null) {
            s.setStart(Date.from(parseIsoInstant(dto.getStart())));
        }
        if (dto.getEnd() != null) {
            s.setEnd(Date.from(parseIsoInstant(dto.getEnd())));
        }

        // Comment
        if (dto.getComment() != null) {
            s.setComment(dto.getComment());
        }

        return s;
    }

    private SlotDto fromFhirSlot(Slot s) {
        SlotDto dto = new SlotDto();
        dto.setId(Long.parseLong(s.getIdElement().getIdPart()));
        dto.setFhirId(s.getIdElement().getIdPart());
        dto.setExternalId(s.getIdElement().getIdPart());

        // Status
        if (s.hasStatus()) {
            dto.setStatus(mapFromSlotStatus(s.getStatus()));
        }

        // Schedule reference -> providerId
        if (s.hasSchedule() && s.getSchedule().hasReference()) {
            String ref = s.getSchedule().getReference();
            if (ref.startsWith("Schedule/")) {
                try {
                    dto.setProviderId(Long.parseLong(ref.substring("Schedule/".length())));
                } catch (NumberFormatException ignored) {}
            }
        }

        // Start/End
        ZoneId zone = ZoneId.systemDefault();
        if (s.hasStart()) {
            dto.setStart(ZonedDateTime.ofInstant(s.getStart().toInstant(), zone)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
        if (s.hasEnd()) {
            dto.setEnd(ZonedDateTime.ofInstant(s.getEnd().toInstant(), zone)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }

        // Comment
        if (s.hasComment()) {
            dto.setComment(s.getComment());
        }

        // Set audit information
        SlotDto.Audit audit = new SlotDto.Audit();
        audit.setCreatedDate(java.time.LocalDateTime.now().toString());
        audit.setLastModifiedDate(java.time.LocalDateTime.now().toString());
        dto.setAudit(audit);

        return dto;
    }

    // -------- Helpers --------

    private void validateMandatoryFields(SlotDto dto) {
        StringBuilder errors = new StringBuilder();
        
        if (dto == null) {
            throw new IllegalArgumentException("Slot data cannot be null");
        }
        
        if (dto.getProviderId() == null) {
            errors.append("providerId, ");
        }
        
        if (dto.getStart() == null || dto.getStart().trim().isEmpty()) {
            errors.append("start, ");
        }
        
        if (dto.getEnd() == null || dto.getEnd().trim().isEmpty()) {
            errors.append("end, ");
        }
        
        if (dto.getStatus() == null || dto.getStatus().trim().isEmpty()) {
            errors.append("status, ");
        }
        
        if (errors.length() > 0) {
            errors.setLength(errors.length() - 2);
            throw new IllegalArgumentException("Missing mandatory fields: " + errors);
        }
    }

    private Instant parseIsoInstant(String iso) {
        try {
            return Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(iso));
        } catch (Exception ignore) {
            return Instant.parse(iso);
        }
    }

    private Slot.SlotStatus mapToSlotStatus(String status) {
        if (status == null) return Slot.SlotStatus.FREE;
        return switch (status.toLowerCase()) {
            case "busy" -> Slot.SlotStatus.BUSY;
            case "busy-unavailable" -> Slot.SlotStatus.BUSYUNAVAILABLE;
            case "busy-tentative" -> Slot.SlotStatus.BUSYTENTATIVE;
            case "entered-in-error" -> Slot.SlotStatus.ENTEREDINERROR;
            default -> Slot.SlotStatus.FREE;
        };
    }

    private String mapFromSlotStatus(Slot.SlotStatus status) {
        if (status == null) return "free";
        return switch (status) {
            case BUSY -> "busy";
            case BUSYUNAVAILABLE -> "busy-unavailable";
            case BUSYTENTATIVE -> "busy-tentative";
            case ENTEREDINERROR -> "entered-in-error";
            default -> "free";
        };
    }
}
