package org.ciyex.ehr.service;

import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.ScheduleDto;
import org.ciyex.ehr.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FHIR-only Schedule Service.
 * Uses FHIR Schedule resource directly via FhirClientService.
 * No local database storage - all data stored in FHIR server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    // Recurrence extension URL
    private static final String EXT_URL_RECURRENCE = "http://ciyex.com/fhir/StructureDefinition/schedule-recurrence";
    private static final String EXT_FREQ = "frequency";
    private static final String EXT_INTERVAL = "interval";
    private static final String EXT_BY_WEEKDAY = "byWeekday";
    private static final String EXT_START_DATE = "startDate";
    private static final String EXT_END_DATE = "endDate";
    private static final String EXT_START_TIME = "startTime";
    private static final String EXT_END_TIME = "endTime";
    private static final String EXT_MAX_OCC = "maxOccurrences";
    private static final String EXT_TZ = "timezone";
    private static final String EXT_LOC_ID = "locationId";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public ScheduleDto create(ScheduleDto dto) {
        if (dto.getProviderId() == null) {
            throw new IllegalArgumentException("providerId is required");
        }
        validateScheduleDto(dto);

        log.debug("Creating FHIR Schedule for provider: {}", dto.getProviderId());

        Schedule schedule = toFhirSchedule(dto);
        var outcome = fhirClientService.create(schedule, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setId(Long.parseLong(fhirId));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);

        // Set audit information
        ScheduleDto.Audit audit = new ScheduleDto.Audit();
        audit.setCreatedDate(java.time.LocalDateTime.now().toString());
        audit.setLastModifiedDate(java.time.LocalDateTime.now().toString());
        dto.setAudit(audit);

        log.info("Created FHIR Schedule with id: {}", fhirId);

        return dto;
    }

    // GET BY ID (FHIR ID)
    public ScheduleDto getById(String fhirId) {
        log.debug("Getting FHIR Schedule: {}", fhirId);
        Schedule schedule = fhirClientService.read(Schedule.class, fhirId, getPracticeId());
        return fromFhirSchedule(schedule);
    }

    // GET ALL
    public ApiResponse<List<ScheduleDto>> getAllSchedules() {
        log.debug("Getting all FHIR Schedules");

        Bundle bundle = fhirClientService.search(Schedule.class, getPracticeId());
        List<Schedule> schedules = fhirClientService.extractResources(bundle, Schedule.class);

        List<ScheduleDto> dtos = schedules.stream()
                .map(this::fromFhirSchedule)
                .collect(Collectors.toList());

        return ApiResponse.<List<ScheduleDto>>builder()
                .success(true)
                .message("Schedules retrieved successfully")
                .data(dtos)
                .build();
    }

    // GET BY PROVIDER
    public List<ScheduleDto> getByProviderId(Long providerId) {
        log.debug("Getting FHIR Schedules for provider: {}", providerId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Schedule.class)
                .where(new ReferenceClientParam("actor").hasId("Practitioner/" + providerId))
                .returnBundle(Bundle.class)
                .execute();

        List<Schedule> schedules = fhirClientService.extractResources(bundle, Schedule.class);
        return schedules.stream().map(this::fromFhirSchedule).collect(Collectors.toList());
    }

    // UPDATE
    public ScheduleDto update(String fhirId, ScheduleDto dto) {
        log.debug("Updating FHIR Schedule: {}", fhirId);

        Schedule schedule = toFhirSchedule(dto);
        schedule.setId(fhirId);
        fhirClientService.update(schedule, getPracticeId());

        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        return dto;
    }

    // DELETE
    public void delete(String fhirId) {
        log.debug("Deleting FHIR Schedule: {}", fhirId);
        fhirClientService.delete(Schedule.class, fhirId, getPracticeId());
    }

    // -------- FHIR Mapping --------

    private Schedule toFhirSchedule(ScheduleDto dto) {
        Schedule s = new Schedule();
        s.setActive(!"inactive".equalsIgnoreCase(dto.getStatus()));

        // Actor: Provider
        if (dto.getProviderId() != null) {
            s.addActor(new Reference("Practitioner/" + dto.getProviderId()));
        }

        // Additional actors
        if (dto.getActorReferences() != null) {
            for (String ref : dto.getActorReferences()) {
                if (dto.getProviderId() != null && ("Practitioner/" + dto.getProviderId()).equals(ref)) continue;
                s.addActor(new Reference(ref));
            }
        }

        // Service category/type/specialty
        if (dto.getServiceCategory() != null) {
            s.addServiceCategory().setText(dto.getServiceCategory());
        }
        if (dto.getServiceType() != null) {
            s.addServiceType().setText(dto.getServiceType());
        }
        if (dto.getSpecialty() != null) {
            s.addSpecialty().setText(dto.getSpecialty());
        }
        if (dto.getComment() != null) {
            s.setComment(dto.getComment());
        }

        // Planning horizon (start/end)
        if (dto.getStart() != null || dto.getEnd() != null) {
            org.hl7.fhir.r4.model.Period planningHorizon = new org.hl7.fhir.r4.model.Period();
            if (dto.getStart() != null) {
                planningHorizon.setStart(Date.from(parseIsoInstant(dto.getStart(), dto.getTimezone())));
            }
            if (dto.getEnd() != null) {
                planningHorizon.setEnd(Date.from(parseIsoInstant(dto.getEnd(), dto.getTimezone())));
            }
            s.setPlanningHorizon(planningHorizon);
        }

        // Recurrence extension
        if (dto.getRecurrence() != null || dto.getTimezone() != null) {
            Extension root = new Extension().setUrl(EXT_URL_RECURRENCE);
            ScheduleDto.Recurrence r = dto.getRecurrence();
            if (r != null) {
                addString(root, EXT_FREQ, r.getFrequency());
                addInteger(root, EXT_INTERVAL, r.getInterval());
                addStringList(root, EXT_BY_WEEKDAY, r.getByWeekday());
                addString(root, EXT_START_DATE, r.getStartDate());
                addString(root, EXT_END_DATE, r.getEndDate());
                addString(root, EXT_START_TIME, r.getStartTime());
                addString(root, EXT_END_TIME, r.getEndTime());
                addInteger(root, EXT_MAX_OCC, r.getMaxOccurrences());
                addString(root, EXT_LOC_ID, r.getLocationId());
            }
            addString(root, EXT_TZ, dto.getTimezone());
            s.addExtension(root);
        }

        return s;
    }

    private ScheduleDto fromFhirSchedule(Schedule s) {
        ScheduleDto dto = new ScheduleDto();
        dto.setFhirId(s.getIdElement().getIdPart());
        dto.setExternalId(s.getIdElement().getIdPart());
        dto.setId(Long.parseLong(s.getIdElement().getIdPart()));
        dto.setStatus(s.getActive() ? "active" : "inactive");

        // Actors
        if (s.hasActor()) {
            List<String> refs = new ArrayList<>();
            for (Reference actor : s.getActor()) {
                String ref = actor.getReference();
                if (ref != null) {
                    if (ref.startsWith("Practitioner/")) {
                        try {
                            dto.setProviderId(Long.parseLong(ref.substring("Practitioner/".length())));
                        } catch (NumberFormatException ignored) {
                            refs.add(ref);
                        }
                    } else {
                        refs.add(ref);
                    }
                }
            }
            if (!refs.isEmpty()) dto.setActorReferences(refs);
        }

        // Service metadata
        if (s.hasServiceCategory()) dto.setServiceCategory(s.getServiceCategoryFirstRep().getText());
        if (s.hasServiceType()) dto.setServiceType(s.getServiceTypeFirstRep().getText());
        if (s.hasSpecialty()) dto.setSpecialty(s.getSpecialtyFirstRep().getText());
        if (s.hasComment()) dto.setComment(s.getComment());

        // Planning horizon
        if (s.hasPlanningHorizon()) {
            org.hl7.fhir.r4.model.Period p = s.getPlanningHorizon();
            ZoneId zone = ZoneId.systemDefault();
            if (p.hasStart()) {
                dto.setStart(ZonedDateTime.ofInstant(p.getStart().toInstant(), zone)
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            }
            if (p.hasEnd()) {
                dto.setEnd(ZonedDateTime.ofInstant(p.getEnd().toInstant(), zone)
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            }
        }

        // Recurrence extension
        Extension root = s.getExtensionByUrl(EXT_URL_RECURRENCE);
        if (root != null) {
            ScheduleDto.Recurrence r = new ScheduleDto.Recurrence();
            r.setFrequency(getString(root, EXT_FREQ));
            r.setInterval(getInteger(root, EXT_INTERVAL));
            r.setByWeekday(getStringList(root, EXT_BY_WEEKDAY));
            r.setStartDate(getString(root, EXT_START_DATE));
            r.setEndDate(getString(root, EXT_END_DATE));
            r.setStartTime(getString(root, EXT_START_TIME));
            r.setEndTime(getString(root, EXT_END_TIME));
            r.setMaxOccurrences(getInteger(root, EXT_MAX_OCC));
            r.setLocationId(getString(root, EXT_LOC_ID));
            String tz = getString(root, EXT_TZ);
            if (tz != null) dto.setTimezone(tz);
            if (hasAnyRecurrence(r)) dto.setRecurrence(r);
        }

        // Set audit information
        ScheduleDto.Audit audit = new ScheduleDto.Audit();
        audit.setCreatedDate(java.time.LocalDateTime.now().toString());
        audit.setLastModifiedDate(java.time.LocalDateTime.now().toString());
        dto.setAudit(audit);

        return dto;
    }

    // -------- Helpers --------

    private void validateScheduleDto(ScheduleDto dto) {
        if (dto.getRecurrence() == null) {
            if (dto.getStart() == null || dto.getEnd() == null || dto.getTimezone() == null) {
                throw new IllegalArgumentException("One-time schedule requires start, end, and timezone");
            }
        } else {
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

    private Instant parseIsoInstant(String iso, String preferredTz) {
        try {
            return Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(iso));
        } catch (Exception ignore) {
            ZoneId zone = preferredTz != null ? ZoneId.of(preferredTz) : ZoneId.systemDefault();
            LocalDateTime ldt = LocalDateTime.parse(iso.replace("Z", "").replaceFirst("\\+.*$", ""));
            return ldt.atZone(zone).toInstant();
        }
    }

    private boolean hasAnyRecurrence(ScheduleDto.Recurrence r) {
        return r.getFrequency() != null || r.getInterval() != null ||
                (r.getByWeekday() != null && !r.getByWeekday().isEmpty()) ||
                r.getStartDate() != null || r.getEndDate() != null ||
                r.getStartTime() != null || r.getEndTime() != null ||
                r.getMaxOccurrences() != null || r.getLocationId() != null;
    }

    private void addString(Extension root, String name, String value) {
        if (value == null) return;
        root.addExtension(new Extension(name, new StringType(value)));
    }

    private void addInteger(Extension root, String name, Integer value) {
        if (value == null) return;
        root.addExtension(new Extension(name, new IntegerType(value)));
    }

    private void addStringList(Extension root, String name, List<String> values) {
        if (values == null || values.isEmpty()) return;
        for (String v : values) root.addExtension(new Extension(name, new StringType(v)));
    }

    private String getString(Extension root, String name) {
        Extension e = root.getExtensionByUrl(name);
        return e != null && e.getValue() instanceof StringType ? ((StringType) e.getValue()).getValue() : null;
    }

    private Integer getInteger(Extension root, String name) {
        Extension e = root.getExtensionByUrl(name);
        return e != null && e.getValue() instanceof IntegerType ? ((IntegerType) e.getValue()).getValue() : null;
    }

    private List<String> getStringList(Extension root, String name) {
        List<String> out = new ArrayList<>();
        for (Extension e : root.getExtension()) {
            if (name.equals(e.getUrl()) && e.getValue() instanceof StringType) {
                out.add(((StringType) e.getValue()).getValue());
            }
        }
        return out.isEmpty() ? null : out;
    }
}
