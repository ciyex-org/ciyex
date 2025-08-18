package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.ScheduleDto;
import com.qiaben.ciyex.dto.core.integration.RequestContext;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Appointment.AppointmentStatus;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@StorageType("fhir")
@Component("fhirExternalScheduleStorage")
@Slf4j
public class FhirExternalScheduleStorage implements ExternalStorage<ScheduleDto> {

    private final FhirResourceStorage fhirResourceStorage;

    public FhirExternalScheduleStorage(FhirResourceStorage fhirResourceStorage) {
        this.fhirResourceStorage = fhirResourceStorage;
        log.info("Initializing FhirExternalScheduleStorage");
    }

    @Transactional
    @Override
    public String create(ScheduleDto dto) {
        requireOrg();
        Appointment appt = toFhir(dto);
        return fhirResourceStorage.create(appt);                         // same pattern used in provider storage :contentReference[oaicite:15]{index=15}L44-L54
    }

    @Transactional
    @Override
    public void update(ScheduleDto dto, String externalId) {
        requireOrg();
        Appointment appt = toFhir(dto);
        fhirResourceStorage.update(appt, externalId);                    // mirrors provider update :contentReference[oaicite:16]{index=16}L115-L127
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
        fhirResourceStorage.delete("Appointment", externalId);           // mirrors provider delete :contentReference[oaicite:17]{index=17}L151-L162
    }

    @Transactional(readOnly = true)
    @Override
    public List<ScheduleDto> searchAll() {
        requireOrg();
        List<Appointment> appts = fhirResourceStorage.searchAll(Appointment.class);
        return appts.stream().map(this::fromFhir).toList();              // mirrors provider searchAll :contentReference[oaicite:18]{index=18}L171-L183
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return ScheduleDto.class.isAssignableFrom(entityType);           // mirrors provider supports :contentReference[oaicite:19]{index=19}L191-L195
    }

    /* ---------------- Helpers ---------------- */

    private void requireOrg() {
        if (RequestContext.get() == null || RequestContext.get().getOrgId() == null) {
            throw new SecurityException("No orgId available in request context"); // same guard style :contentReference[oaicite:20]{index=20}L57-L70
        }
    }

    private Appointment toFhir(ScheduleDto dto) {
        Appointment a = new Appointment();
        a.setStatus(AppointmentStatus.BOOKED);
        // Put a simple CodeableConcept summary; you can elaborate with participants etc.
        if (dto.getTitle() != null) {
            a.addReasonCode(new CodeableConcept().setText(dto.getTitle()));
        }
        if (dto.getLocation() != null) {
            a.addReasonCode(new CodeableConcept().setText("Location: " + dto.getLocation()));
        }
        // For wireframe purposes, encode start/end via minutes from start
        LocalDateTime start = LocalDateTime.parse(dto.getStartDate() + "T" + dto.getStartTime() + ":00");
        LocalDateTime end = start.plusMinutes(dto.getDurationMin() != null ? dto.getDurationMin() : 30);
        a.setStart(java.util.Date.from(start.atZone(java.time.ZoneId.systemDefault()).toInstant()));
        a.setEnd(java.util.Date.from(end.atZone(java.time.ZoneId.systemDefault()).toInstant()));
        return a;
    }

    private ScheduleDto fromFhir(Appointment a) {
        DateTimeFormatter d = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter t = DateTimeFormatter.ofPattern("HH:mm");
        LocalDateTime start = LocalDateTime.ofInstant(a.getStart().toInstant(), java.time.ZoneId.systemDefault());
        LocalDateTime end = LocalDateTime.ofInstant(a.getEnd().toInstant(), java.time.ZoneId.systemDefault());

        ScheduleDto dto = new ScheduleDto();
        dto.setTitle(a.getReasonCode().isEmpty() ? null : a.getReasonCodeFirstRep().getText());
        dto.setStartDate(d.format(start));
        dto.setStartTime(t.format(start));
        dto.setDurationMin((int) java.time.Duration.between(start, end).toMinutes());
        dto.setOrgId(RequestContext.get() != null ? RequestContext.get().getOrgId() : null);       // same mapping idea :contentReference[oaicite:21]{index=21}L205-L212
        return dto;
    }
}
