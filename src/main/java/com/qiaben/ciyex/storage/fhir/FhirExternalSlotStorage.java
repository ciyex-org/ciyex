package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.SlotDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.storage.ExternalSlotStorage;
import com.qiaben.ciyex.storage.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Slot;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@StorageType("fhir")
@Component("fhirExternalSlotStorage")
@Slf4j
public class FhirExternalSlotStorage implements ExternalSlotStorage {

    private final FhirResourceStorage fhirResourceStorage;

    public FhirExternalSlotStorage(FhirResourceStorage fhirResourceStorage) {
        this.fhirResourceStorage = fhirResourceStorage;
        log.info("Initializing FhirExternalSlotStorage with FhirResourceStorage");
    }

    @Override
    public String createSlot(SlotDto dto) {
        return create(dto);
    }

    @Override
    public void updateSlot(SlotDto dto, String externalId) {
        update(dto, externalId);
    }

    @Override
    public SlotDto getSlot(String externalId) {
        return get(externalId);
    }

    @Override
    public void deleteSlot(String externalId) {
        delete(externalId);
    }

    @Override
    public List<SlotDto> searchAllSlots() {
        return searchAll();
    }

    @Transactional
    public String create(SlotDto dto) {
        String currentTenantName = requireTenantName();
        log.debug("Creating Slot for orgId={}, dto={}", currentTenantName, dto);

        Slot slot = toFhir(dto);
        String externalId;
        try {
            externalId = fhirResourceStorage.create(slot);
            log.info("Successfully created Slot in external storage with externalId={} for orgId={}", externalId, currentTenantName);
        } catch (Exception e) {
            log.error("Failed to create Slot in external storage for orgId={}, error={}", currentTenantName, e.getMessage(), e);
            throw new RuntimeException("Failed to sync with external storage", e);
        }
        return externalId;
    }

    @Transactional
    public void update(SlotDto dto, String externalId) {
        String currentTenantName = requireTenantName();
        log.debug("Updating Slot with externalId={} for orgId={}, dto={}", externalId, currentTenantName, dto);

        Slot slot = toFhir(dto);
        slot.setId(externalId);
        try {
            fhirResourceStorage.update(slot, externalId);
            log.info("Successfully updated Slot in external storage with externalId={} for orgId={}", externalId, currentTenantName);
        } catch (Exception e) {
            log.error("Failed to update Slot in external storage for orgId={}, externalId={}, error={}", currentTenantName, externalId, e.getMessage(), e);
            throw new RuntimeException("Failed to sync with external storage", e);
        }
    }

    @Transactional(readOnly = true)
    public SlotDto get(String externalId) {
        String currentTenantName = requireTenantName();
        log.debug("Fetching Slot with externalId={} for orgId={}", externalId, currentTenantName);

        Slot slot = fhirResourceStorage.get(Slot.class, externalId);
        if (slot == null) {
            log.warn("No FHIR Slot found with externalId={} for orgId={}", externalId, currentTenantName);
            throw new RuntimeException("Slot not found with externalId: " + externalId);
        }
        SlotDto dto = fromFhir(slot);
        log.info("Retrieved SlotDto with externalId={} for orgId={}", externalId, currentTenantName);
        return dto;
    }

    @Transactional
    public void delete(String externalId) {
        String currentTenantName = requireTenantName();
        log.debug("Deleting Slot with externalId={} for orgId={}", externalId, currentTenantName);

        try {
            fhirResourceStorage.delete("Slot", externalId);
            log.info("Successfully deleted Slot in external storage with externalId={} for orgId={}", externalId, currentTenantName);
        } catch (Exception e) {
            log.error("Failed to delete Slot in external storage for orgId={}, externalId={}, error={}", currentTenantName, externalId, e.getMessage(), e);
            throw new RuntimeException("Failed to sync with external storage", e);
        }
    }

    @Transactional(readOnly = true)
    public List<SlotDto> searchAll() {
        String currentTenantName = requireTenantName();
        log.debug("Searching all Slots for orgId={}", currentTenantName);

        List<Slot> slots = fhirResourceStorage.searchAll(Slot.class);
        log.debug("Retrieved {} Slots from external storage for orgId={}", slots.size(), currentTenantName);

        List<SlotDto> dtos = slots.stream().map(this::fromFhir).collect(Collectors.toList());
        log.info("Returning {} SlotDtos for orgId={}", dtos.size(), currentTenantName);
        return dtos;
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return SlotDto.class.isAssignableFrom(entityType);
    }

    /* --- Mapping --- */

    private String requireTenantName() {
        String tenantName = RequestContext.get() != null ? RequestContext.get().getTenantName() : null;
        if (tenantName == null) {
            log.warn("tenantName is null in RequestContext");
            throw new SecurityException("No tenantName available in request context");
        }
        return tenantName;
    }

    private Slot toFhir(SlotDto dto) {
        log.debug("Mapping SlotDto to FHIR Slot: {}", dto);
        Slot slot = new Slot();

        if (dto.getProviderId() != null) {
            slot.setSchedule(new Reference("Schedule/" + dto.getProviderId()));
            log.debug("Mapped providerId={} to Schedule reference", dto.getProviderId());
        }

        if (dto.getStart() != null) {
            slot.setStart(parseIsoInstant(dto.getStart()));
            log.debug("Mapped start={}", dto.getStart());
        }
        if (dto.getEnd() != null) {
            slot.setEnd(parseIsoInstant(dto.getEnd()));
            log.debug("Mapped end={}", dto.getEnd());
        }

        if (dto.getStatus() != null) {
            try {
                slot.setStatus(Slot.SlotStatus.fromCode(dto.getStatus()));
                log.debug("Mapped status={}", dto.getStatus());
            } catch (Exception e) {
                slot.setStatus(Slot.SlotStatus.BUSY);
                log.warn("Invalid status={} in dto, defaulted to BUSY", dto.getStatus());
            }
        }

        if (dto.getComment() != null) {
            slot.setComment(dto.getComment());
            log.debug("Mapped comment={}", dto.getComment());
        }

        // ⬇️ Add this block at the end of toFhir()
        String currentTenantName = requireTenantName();
        slot.getMeta().addTag()
                .setSystem("http://ciyex.org/tenant")
                .setCode(currentTenantName)
                .setDisplay("Tenant Org " + currentTenantName);
        return slot;
    }

    private SlotDto fromFhir(Slot s) {
        String externalId = s.getIdElement() != null ? s.getIdElement().getIdPart() : null;
        log.debug("Mapping FHIR Slot to SlotDto, externalId={}", externalId);

        SlotDto dto = new SlotDto();
        dto.setExternalId(externalId);
        dto.setTenantName(RequestContext.get() != null ? RequestContext.get().getTenantName() : null);

        if (s.hasStart()) {
            String start = ZonedDateTime.ofInstant(s.getStart().toInstant(), ZoneId.systemDefault())
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            dto.setStart(start);
            log.debug("Mapped start={}", start);
        }
        if (s.hasEnd()) {
            String end = ZonedDateTime.ofInstant(s.getEnd().toInstant(), ZoneId.systemDefault())
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            dto.setEnd(end);
            log.debug("Mapped end={}", end);
        }

        if (s.hasStatus()) {
            dto.setStatus(s.getStatus().toCode());
            log.debug("Mapped status={}", dto.getStatus());
        }
        if (s.hasComment()) {
            dto.setComment(s.getComment());
            log.debug("Mapped comment={}", dto.getComment());
        }

        if (s.hasSchedule() && s.getSchedule().getReference() != null) {
            String ref = s.getSchedule().getReference();
            if (ref.startsWith("Schedule/")) {
                try {
                    dto.setProviderId(Long.parseLong(ref.substring("Schedule/".length())));
                    log.debug("Mapped schedule reference {} to providerId={}", ref, dto.getProviderId());
                } catch (NumberFormatException e) {
                    log.warn("Invalid schedule reference format: {}", ref);
                }
            }
        }
        return dto;
    }

    private Date parseIsoInstant(String iso) {
        try {
            Instant instant = Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(iso));
            Date parsed = Date.from(instant);
            log.debug("Parsed ISO instant={} to Date={}", iso, parsed);
            return parsed;
        } catch (Exception e) {
            LocalDateTime ldt = LocalDateTime.parse(iso.replace("Z","").replaceFirst("\\+.*$",""));
            Date fallback = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
            log.warn("Fallback parsing for iso={} resulted in Date={}", iso, fallback);
            return fallback;
        }
    }
}
