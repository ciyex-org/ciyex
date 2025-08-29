package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import com.qiaben.ciyex.dto.AppointmentDTO;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalAppointmentStorage;
import com.qiaben.ciyex.storage.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@StorageType("fhir")
@Component("fhirExternalAppointmentStorage")
@Slf4j
public class FhirExternalAppointmentStorage implements ExternalAppointmentStorage {

    private final FhirClientProvider fhirClientProvider;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    public FhirExternalAppointmentStorage(FhirClientProvider fhirClientProvider) {
        this.fhirClientProvider = fhirClientProvider;
        log.info("Initializing FhirExternalAppointmentStorage");
    }

    @Override
    public String create(AppointmentDTO dto) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            Appointment fhirAppointment = mapToFhir(dto);
            String externalId = client.create().resource(fhirAppointment).execute().getId().getIdPart();
            log.info("Created Appointment with externalId {} for orgId {}", externalId, orgId);
            return externalId;
        });
    }

    @Override
    public void update(AppointmentDTO dto, String externalId) {
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            Appointment fhirAppointment = mapToFhir(dto);
            fhirAppointment.setId(externalId);
            client.update().resource(fhirAppointment).execute();
            log.info("Updated Appointment {} in FHIR", externalId);
            return null;
        });
    }

    @Override
    public AppointmentDTO get(String externalId) {
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            Appointment fhirAppointment = client.read().resource(Appointment.class).withId(externalId).execute();
            return mapFromFhir(fhirAppointment);
        });
    }

    @Override
    public void delete(String externalId) {
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            client.delete().resourceById("Appointment", externalId).execute();
            log.info("Deleted Appointment {} in FHIR", externalId);
            return null;
        });
    }

    @Override
    public List<AppointmentDTO> searchAll() {
        Bundle bundle = fhirClientProvider.getForCurrentOrg()
                .search()
                .forResource(Appointment.class)
                .returnBundle(Bundle.class)
                .execute();

        return bundle.getEntry().stream()
                .map(entry -> (Appointment) entry.getResource())
                .map(this::mapFromFhir)
                .collect(Collectors.toList());
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return AppointmentDTO.class.isAssignableFrom(entityType);
    }

    // 🔹 Mapping Helpers

    private Appointment mapToFhir(AppointmentDTO dto) {
        Appointment fhir = new Appointment();

        // Status
        fhir.setStatus(mapStatus(dto.getStatus()));

        // Visit type
        if (dto.getVisitType() != null) {
            fhir.addServiceCategory().setText(dto.getVisitType());
        }

        // Priority (PositiveIntType in R4)
        if (dto.getPriority() != null) {
            int priorityValue = dto.getPriority().equalsIgnoreCase("urgent") ? 1 : 5;
            fhir.setPriority(priorityValue);
        }

        // Reason
        if (dto.getReason() != null) {
            fhir.addReasonCode(new CodeableConcept().setText(dto.getReason()));
        }

        // Start / End
        try {
            if (dto.getAppointmentStartDate() != null && dto.getAppointmentStartTime() != null) {
                String startIso = dto.getAppointmentStartDate() + "T" + dto.getAppointmentStartTime() + ":00";
                LocalDateTime start = LocalDateTime.parse(startIso, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                fhir.setStart(Date.from(start.toInstant(ZoneOffset.UTC)));
            }

            if (dto.getAppointmentEndDate() != null && dto.getAppointmentEndTime() != null) {
                String endIso = dto.getAppointmentEndDate() + "T" + dto.getAppointmentEndTime() + ":00";
                LocalDateTime end = LocalDateTime.parse(endIso, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                fhir.setEnd(Date.from(end.toInstant(ZoneOffset.UTC)));
            }
        } catch (Exception e) {
            log.warn("Failed to parse appointment start/end date", e);
        }

        // Participant: Patient
        if (dto.getPatientId() != null) {
            fhir.addParticipant()
                    .setActor(new Reference("Patient/" + dto.getPatientId()))
                    .setStatus(Appointment.ParticipationStatus.ACCEPTED);
        }

        // Participant: Provider
        if (dto.getProviderId() != null) {
            fhir.addParticipant()
                    .setActor(new Reference("Practitioner/" + dto.getProviderId()))
                    .setStatus(Appointment.ParticipationStatus.ACCEPTED);
        }

        // Participant: Location
        if (dto.getLocationId() != null) {
            fhir.addParticipant()
                    .setActor(new Reference("Location/" + dto.getLocationId()))
                    .setStatus(Appointment.ParticipationStatus.ACCEPTED);
        }

        return fhir;
    }

    private Appointment.AppointmentStatus mapStatus(String status) {
        if (status == null) return Appointment.AppointmentStatus.PROPOSED;

        switch (status.toLowerCase()) {
            case "scheduled":
                return Appointment.AppointmentStatus.BOOKED;
            case "confirmed":
                return Appointment.AppointmentStatus.BOOKED;
            case "checked-in":
                return Appointment.AppointmentStatus.CHECKEDIN;
            case "completed":
                return Appointment.AppointmentStatus.FULFILLED;
            default:
                return Appointment.AppointmentStatus.PROPOSED;
        }
    }

    private AppointmentDTO mapFromFhir(Appointment fhir) {
        AppointmentDTO dto = new AppointmentDTO();
        dto.setId(fhir.getIdElement().hasIdPart() ? Long.valueOf(fhir.getIdElement().getIdPart()) : null);

        if (!fhir.getServiceCategory().isEmpty()) {
            dto.setVisitType(fhir.getServiceCategoryFirstRep().getText());
        }
        if (fhir.hasStatus()) {
            switch (fhir.getStatus()) {
                case BOOKED:
                    dto.setStatus("Scheduled");
                    break;
                case CHECKEDIN:
                    dto.setStatus("Checked-in");
                    break;
                case FULFILLED:
                    dto.setStatus("Completed");
                    break;
                default:
                    dto.setStatus("Scheduled");
            }
        }
        if (fhir.hasPriority()) {
            dto.setPriority(fhir.getPriority() == 1 ? "Urgent" : "Routine");
        }
        if (!fhir.getReasonCode().isEmpty()) {
            dto.setReason(fhir.getReasonCodeFirstRep().getText());
        }
        if (fhir.hasStart()) {
            dto.setAppointmentStartDate(DATE_FORMAT.format(fhir.getStart()));
            dto.setAppointmentStartTime(new SimpleDateFormat("HH:mm").format(fhir.getStart()));
        }
        if (fhir.hasEnd()) {
            dto.setAppointmentEndDate(DATE_FORMAT.format(fhir.getEnd()));
            dto.setAppointmentEndTime(new SimpleDateFormat("HH:mm").format(fhir.getEnd()));
        }

        fhir.getParticipant().forEach(p -> {
            if (p.hasActor() && p.getActor().getReference() != null) {
                String ref = p.getActor().getReference();
                if (ref.startsWith("Patient/")) {
                    dto.setPatientId(Long.valueOf(ref.replace("Patient/", "")));
                } else if (ref.startsWith("Practitioner/")) {
                    dto.setProviderId(Long.valueOf(ref.replace("Practitioner/", "")));
                } else if (ref.startsWith("Location/")) {
                    try {
                        dto.setLocationId(Long.valueOf(ref.replace("Location/", "")));
                    } catch (NumberFormatException e) {
                        log.warn("FHIR Location reference {} is not numeric", ref);
                    }
                }
            }
        });

        return dto;
    }

    private <T> T executeWithRetry(FhirOperation<T> operation) {
        try {
            return operation.execute();
        } catch (FhirClientConnectionException e) {
            if (e.getStatusCode() == 401) {
                log.warn("401 Unauthorized — retrying with fresh FHIR client");
                return operation.execute();
            }
            throw e;
        } catch (Exception e) {
            log.error("Unexpected exception in FHIR operation", e);
            throw e;
        }
    }

    @FunctionalInterface
    private interface FhirOperation<T> {
        T execute();
    }
}
