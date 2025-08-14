package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import com.qiaben.ciyex.dto.SlotDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@StorageType("fhir")
@Component("fhirExternalSlotStorage")
@Slf4j
public class FhirExternalSlotStorage implements ExternalStorage<SlotDto> {

    private final FhirClientProvider fhirClientProvider;
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"); // ISO 8601 with offset

    @Autowired
    public FhirExternalSlotStorage(FhirClientProvider fhirClientProvider) {
        this.fhirClientProvider = fhirClientProvider;
        log.info("Initializing FhirExternalSlotStorage with FhirClientProvider");
    }

    private IGenericClient getClient() {
        return fhirClientProvider.getForCurrentOrg();
    }

    @Override
    public String create(SlotDto entityDto) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.info("Entering create for orgId: {}, slot startTime: {}", orgId, entityDto.getStartTime());
        return executeWithRetry(() -> {
            IGenericClient client = getClient();
            log.debug("Fetched IGenericClient for orgId: {}", orgId);
            Slot fhirSlot = mapToFhirSlot(entityDto);
            log.debug("Mapped SlotDto to FHIR Slot: start={}, end={}, status={}", fhirSlot.getStart(), fhirSlot.getEnd(), fhirSlot.getStatus());
            String externalId = client.create().resource(fhirSlot).execute().getId().getIdPart();
            log.info("Created Slot with externalId: {} for orgId: {}", externalId, orgId);
            return externalId;
        });
    }

    @Override
    public void update(SlotDto entityDto, String externalId) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.info("Entering update for orgId: {}, externalId: {}, slot startTime: {}", orgId, externalId, entityDto.getStartTime());
        executeWithRetry(() -> {
            IGenericClient client = getClient();
            log.debug("Fetched IGenericClient for orgId: {}", orgId);
            Slot fhirSlot = mapToFhirSlot(entityDto);
            fhirSlot.setId(externalId);
            log.debug("Updating FHIR Slot with id: {}, start={}, end={}, status={}", externalId, fhirSlot.getStart(), fhirSlot.getEnd(), fhirSlot.getStatus());
            client.update().resource(fhirSlot).execute();
            log.info("Updated Slot with externalId: {} for orgId: {}", externalId, orgId);
            return null;
        });
    }

    @Override
    public SlotDto get(String externalId) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.info("Entering get for orgId: {}, externalId: {}", orgId, externalId);
        return executeWithRetry(() -> {
            IGenericClient client = getClient();
            log.debug("Fetched IGenericClient for orgId: {}", orgId);
            Slot fhirSlot = client.read().resource(Slot.class).withId(externalId).execute();
            log.debug("Retrieved FHIR Slot with id: {}, start={}, end={}, status={}", externalId, fhirSlot.getStart(), fhirSlot.getEnd(), fhirSlot.getStatus());
            SlotDto slotDto = mapFromFhirSlot(fhirSlot, client);
            log.info("Retrieved SlotDto with externalId: {} for orgId: {}", externalId, orgId);
            log.debug("Mapped SlotDto: externalId={}, startTime={}, endTime={}, status={}", slotDto.getExternalId(), slotDto.getStartTime(), slotDto.getEndTime(), slotDto.getStatus());
            return slotDto;
        });
    }

    @Override
    public void delete(String externalId) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.info("Entering delete for orgId: {}, externalId: {}", orgId, externalId);
        executeWithRetry(() -> {
            IGenericClient client = getClient();
            log.debug("Fetched IGenericClient for orgId: {}", orgId);
            log.info("Deleting Slot with externalId: {} for orgId: {}", externalId, orgId);
            client.delete().resourceById("Slot", externalId).execute();
            log.info("Deleted Slot with externalId: {} for orgId: {}", externalId, orgId);
            return null;
        });
    }

    @Override
    public List<SlotDto> searchAll() {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.info("Entering searchAll for orgId: {}", orgId);
        if (orgId == null) {
            log.warn("orgId is null in RequestContext, defaulting to no filtering");
        }

        IGenericClient client = getClient();
        Bundle bundle = client.search()
                .forResource(org.hl7.fhir.r4.model.Slot.class)
                .where(new TokenClientParam("_tag").exactly().systemAndCode("http://ciyex.com/tenant", orgId != null ? orgId.toString() : ""))
                .returnBundle(Bundle.class)
                .execute();

        log.debug("Received Bundle with {} entries for orgId: {}", bundle.getEntry().size(), orgId);
        List<SlotDto> slotDtos = bundle.getEntry().stream()
                .map(entry -> {
                    Slot slot = (Slot) entry.getResource();
                    log.debug("Processing Slot entry: id={}, start={}, end={}", slot.getIdElement().getIdPart(), slot.getStart(), slot.getEnd());
                    SlotDto dto = new SlotDto();
                    dto.setExternalId(slot.getIdElement().getIdPart());
                    // Resolve Schedule to get actors
                    if (slot.hasSchedule()) {
                        String scheduleId = slot.getSchedule().getReference();
                        Schedule schedule = (Schedule) client.read().resource(Schedule.class).withId(scheduleId.replace("Schedule/", "")).execute();
                        if (schedule.hasActor()) {
                            for (Reference actor : schedule.getActor()) {
                                if (actor.getReference().contains("Practitioner")) {
                                    dto.setProviderId(Long.parseLong(actor.getReference().replace("Practitioner/", "")));
                                } else if (actor.getReference().contains("Location")) {
                                    dto.setLocationId(Long.parseLong(actor.getReference().replace("Location/", "")));
                                }
                            }
                        }
                    }
                    dto.setStartTime(slot.getStart().toString());
                    dto.setEndTime(slot.getEnd().toString());
                    dto.setStatus(slot.getStatus().toCode());
                    dto.setOrgId(orgId);
                    log.debug("Mapped SlotDto: externalId={}, startTime={}, endTime={}, status={}", dto.getExternalId(), dto.getStartTime(), dto.getEndTime(), dto.getStatus());
                    return dto;
                })
                .collect(Collectors.toList());

        log.info("Retrieved {} slots for orgId: {} after mapping", slotDtos.size(), orgId);
        return slotDtos;
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return SlotDto.class.isAssignableFrom(entityType);
    }

    private <T> T executeWithRetry(FhirOperation<T> operation) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.debug("Entering executeWithRetry for orgId: {}", orgId);
        try {
            T result = operation.execute();
            log.debug("executeWithRetry succeeded for orgId: {}", orgId);
            return result;
        } catch (FhirClientConnectionException e) {
            log.error("FhirClientConnectionException for orgId: {} with status: {}, message: {}", orgId, e.getStatusCode(), e.getMessage());
            if (e.getStatusCode() == 401) {
                log.warn("Received 401, retrying with fresh FHIR client for orgId: {}", orgId);
                return operation.execute();
            }
            throw e;
        } catch (Exception e) {
            log.error("Unexpected exception in executeWithRetry for orgId: {}, message: {}, stacktrace: {}", orgId, e.getMessage(), e);
            throw e;
        }
    }

    @FunctionalInterface
    private interface FhirOperation<T> {
        T execute();
    }

    private Slot mapToFhirSlot(SlotDto slotDto) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.debug("Mapping SlotDto to FHIR Slot for orgId: {}, startTime: {}", orgId, slotDto.getStartTime());
        Slot fhirSlot = new Slot();
        try {
            java.util.Date startDate = DATE_TIME_FORMAT.parse(slotDto.getStartTime());
            java.util.Date endDate = DATE_TIME_FORMAT.parse(slotDto.getEndTime());
            fhirSlot.setStart(startDate);
            fhirSlot.setEnd(endDate);
        } catch (ParseException e) {
            log.error("Failed to parse startTime or endTime: {}, {}", slotDto.getStartTime(), slotDto.getEndTime(), e);
            throw new RuntimeException("Invalid date/time format", e);
        }
        fhirSlot.setStatus(Slot.SlotStatus.fromCode(slotDto.getStatus()));
        // Reference to an existing Schedule (assumed to be created separately)
        Reference scheduleRef = new Reference("Schedule/schedule-1"); // Placeholder, replace with actual Schedule ID
        fhirSlot.setSchedule(scheduleRef);
        log.debug("Mapped FHIR Slot for orgId: {}, start={}, end={}, status={}", orgId, fhirSlot.getStart(), fhirSlot.getEnd(), fhirSlot.getStatus());
        return fhirSlot;
    }

    private SlotDto mapFromFhirSlot(Slot fhirSlot, IGenericClient client) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.debug("Mapping FHIR Slot to SlotDto for orgId: {}, id: {}, start: {}", orgId, fhirSlot.getIdElement().getIdPart(), fhirSlot.getStart());
        SlotDto dto = new SlotDto();
        dto.setExternalId(fhirSlot.getIdElement().getIdPart());
        dto.setStartTime(fhirSlot.getStart().toString());
        dto.setEndTime(fhirSlot.getEnd().toString());
        dto.setStatus(fhirSlot.getStatus().toCode());
        // Resolve Schedule to get actors
        if (fhirSlot.hasSchedule()) {
            String scheduleId = fhirSlot.getSchedule().getReference();
            Schedule schedule = (Schedule) client.read().resource(Schedule.class).withId(scheduleId.replace("Schedule/", "")).execute();
            if (schedule.hasActor()) {
                for (Reference actor : schedule.getActor()) {
                    if (actor.getReference().contains("Practitioner")) {
                        dto.setProviderId(Long.parseLong(actor.getReference().replace("Practitioner/", "")));
                    } else if (actor.getReference().contains("Location")) {
                        dto.setLocationId(Long.parseLong(actor.getReference().replace("Location/", "")));
                    }
                }
            }
        }
        dto.setOrgId(orgId);
        log.debug("Mapped SlotDto for orgId: {}, externalId: {}, startTime: {}, endTime: {}, status: {}",
                orgId, dto.getExternalId(), dto.getStartTime(), dto.getEndTime(), dto.getStatus());
        return dto;
    }
}