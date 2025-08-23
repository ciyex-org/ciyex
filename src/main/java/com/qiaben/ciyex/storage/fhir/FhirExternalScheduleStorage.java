package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.ScheduleDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.storage.ExternalScheduleStorage;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@StorageType("fhir")
@Component("fhirExternalScheduleStorage")
@Slf4j
public class FhirExternalScheduleStorage implements ExternalScheduleStorage {

    private final FhirResourceStorage fhirResourceStorage;

    public FhirExternalScheduleStorage(FhirResourceStorage fhirResourceStorage) {
        this.fhirResourceStorage = fhirResourceStorage;
        log.info("Initializing FhirExternalScheduleStorage (Appointment-backed)");
    }

    /* ===== Delegate ExternalScheduleStorage methods ===== */

    @Override
    public String createSchedule(ScheduleDto scheduleDto) {
        return create(scheduleDto);
    }

    @Override
    public void updateSchedule(ScheduleDto scheduleDto, String externalId) {
        update(scheduleDto, externalId);
    }

    @Override
    public ScheduleDto getSchedule(String externalId) {
        return get(externalId);
    }

    @Override
    public void deleteSchedule(String externalId) {
        delete(externalId);
    }

    @Override
    public List<ScheduleDto> searchAllSchedules() {
        return searchAll();
    }

    // Recurrence extension namespace + child keys (maps to ScheduleDto.Recurrence)
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

    @Transactional
    @Override
    public String create(ScheduleDto dto) {
        requireOrg();
        Appointment appt = toFhir(dto);
        return fhirResourceStorage.create(appt);
    }

    @Transactional
    @Override
    public void update(ScheduleDto dto, String externalId) {
        requireOrg();
        Appointment appt = toFhir(dto);
        appt.setId(externalId);
        fhirResourceStorage.update(appt, externalId);
    }

    @Transactional(readOnly = true)
    @Override
    public ScheduleDto get(String externalId) {
        requireOrg();
        Appointment appt = fhirResourceStorage.get(Appointment.class, externalId);
        return fromFhir(appt);
    }

    @Transactional
    @Override
    public void delete(String externalId) {
        requireOrg();
        fhirResourceStorage.delete("Appointment", externalId);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ScheduleDto> searchAll() {
        requireOrg();
        List<Appointment> appts = fhirResourceStorage.searchAll(Appointment.class);
        return appts.stream().map(this::fromFhir).toList();
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return ScheduleDto.class.isAssignableFrom(entityType);
    }

    /* ---------------- Helpers ---------------- */

    private void requireOrg() {
        if (RequestContext.get() == null || RequestContext.get().getOrgId() == null) {
            throw new SecurityException("No orgId available in request context");
        }
    }

    private Appointment toFhir(ScheduleDto dto) {
        Appointment a = new Appointment();

        // status
        if ("inactive".equalsIgnoreCase(dto.getStatus())) {
            a.setStatus(Appointment.AppointmentStatus.CANCELLED);
        } else {
            a.setStatus(Appointment.AppointmentStatus.BOOKED);
        }

        // participants: provider + other actors (Location, etc.)
        if (dto.getProviderId() != null) {
            a.addParticipant().setActor(new Reference("Practitioner/" + dto.getProviderId()));
        }
        if (dto.getActorReferences() != null) {
            for (String ref : dto.getActorReferences()) {
                if (dto.getProviderId() != null && ("Practitioner/" + dto.getProviderId()).equals(ref)) continue;
                a.addParticipant().setActor(new Reference(ref));
            }
        }

        // metadata
        if (dto.getServiceCategory() != null) a.addServiceCategory().setText(dto.getServiceCategory());
        if (dto.getServiceType() != null)     a.addServiceType().getTextElement().setValue(dto.getServiceType());
        if (dto.getSpecialty() != null)       a.addSpecialty().setText(dto.getSpecialty());
        if (dto.getComment() != null)         a.setComment(dto.getComment());

        // start/end: ISO-8601 preferred (with offset). Fall back to system zone if offset missing.
        if (dto.getStart() != null) {
            Instant start = parseIsoInstant(dto.getStart(), dto.getTimezone());
            a.setStart(Date.from(start));
        }
        if (dto.getEnd() != null) {
            Instant end = parseIsoInstant(dto.getEnd(), dto.getTimezone());
            a.setEnd(Date.from(end));
        }

        // recurrence + timezone extension (optional)
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
            a.addExtension(root);
        }

        return a;
    }

    private ScheduleDto fromFhir(Appointment a) {
        ZoneId zone = zoneOrSystem(null);

        ScheduleDto dto = new ScheduleDto();
        dto.setExternalId(a.getIdElement() != null ? a.getIdElement().getIdPart() : null);
        dto.setOrgId(RequestContext.get() != null ? RequestContext.get().getOrgId() : null);

        // status
        dto.setStatus(a.getStatus() == Appointment.AppointmentStatus.CANCELLED ? "inactive" : "active");

        // start/end back to ISO-8601 with offset
        if (a.hasStart()) {
            dto.setStart(ZonedDateTime.ofInstant(a.getStart().toInstant(), zone)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
        if (a.hasEnd()) {
            dto.setEnd(ZonedDateTime.ofInstant(a.getEnd().toInstant(), zone)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }

        // participants → providerId + actorReferences
        if (a.hasParticipant()) {
            List<String> refs = a.getParticipant().stream()
                    .map(p -> p.getActor() != null ? p.getActor().getReference() : null)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            dto.setActorReferences(new ArrayList<>());
            for (String ref : refs) {
                if (ref.startsWith("Practitioner/")) {
                    try {
                        dto.setProviderId(Long.parseLong(ref.substring("Practitioner/".length())));
                    } catch (NumberFormatException ignored) {}
                } else {
                    dto.getActorReferences().add(ref);
                }
            }
            if (dto.getActorReferences().isEmpty()) dto.setActorReferences(null);
        }

        // metadata
        if (a.hasServiceCategory()) dto.setServiceCategory(a.getServiceCategoryFirstRep().getText());
        if (a.hasServiceType())     dto.setServiceType(a.getServiceTypeFirstRep().getText());
        if (a.hasSpecialty())       dto.setSpecialty(a.getSpecialtyFirstRep().getText());
        if (a.hasComment())         dto.setComment(a.getComment());

        // recurrence + timezone extension
        Extension root = a.getExtensionByUrl(EXT_URL_RECURRENCE);
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

        return dto;
    }

    /* ----- small utils ----- */

    private Instant parseIsoInstant(String iso, String preferredTz) {
        try {
            // parse with offset if provided
            return Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(iso));
        } catch (Exception ignore) {
            // no offset? assume preferred tz (or system)
            ZoneId zone = zoneOrSystem(preferredTz);
            LocalDateTime ldt = LocalDateTime.parse(iso.replace("Z","").replaceFirst("\\+.*$",""));
            return ldt.atZone(zone).toInstant();
        }
    }

    private ZoneId zoneOrSystem(String tz) {
        try { return tz != null ? ZoneId.of(tz) : ZoneId.systemDefault(); }
        catch (Exception e) { return ZoneId.systemDefault(); }
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
