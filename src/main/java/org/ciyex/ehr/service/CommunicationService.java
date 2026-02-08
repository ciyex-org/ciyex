package org.ciyex.ehr.service;

import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.CommunicationDto;
import org.ciyex.ehr.dto.CommunicationStatus;
import org.ciyex.ehr.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FHIR-only Communication Service.
 * Uses FHIR Communication resource directly via FhirClientService.
 * No local database storage - all data stored in FHIR server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommunicationService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;
    private final PatientService patientService;

    // Extension URLs for custom fields
    private static final String EXT_MESSAGE_TYPE = "http://ciyex.com/fhir/StructureDefinition/message-type";
    private static final String EXT_FROM_TYPE = "http://ciyex.com/fhir/StructureDefinition/from-type";
    private static final String EXT_FROM_NAME = "http://ciyex.com/fhir/StructureDefinition/from-name";
    private static final String EXT_READ_AT = "http://ciyex.com/fhir/StructureDefinition/read-at";
    private static final String EXT_READ_BY = "http://ciyex.com/fhir/StructureDefinition/read-by";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public CommunicationDto create(CommunicationDto dto) {
        if (dto.getPatientId() == null) {
            throw new IllegalArgumentException("Patient ID is required");
        }
        if (dto.getProviderId() == null) {
            throw new IllegalArgumentException("Provider ID is required");
        }
        if (dto.getPayload() == null || dto.getPayload().trim().isEmpty()) {
            throw new IllegalArgumentException("Message payload cannot be empty");
        }

        log.debug("Creating FHIR Communication for patient: {} provider: {}", dto.getPatientId(), dto.getProviderId());

        Communication comm = toFhirCommunication(dto);
        var outcome = fhirClientService.create(comm, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setId(parseFhirIdToLong(fhirId));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        log.info("Created FHIR Communication with id: {}", fhirId);

        return dto;
    }

    // GET BY ID
    public CommunicationDto getById(String fhirId) {
        log.debug("Getting FHIR Communication: {}", fhirId);
        Communication comm = fhirClientService.read(Communication.class, fhirId, getPracticeId());
        return fromFhirCommunication(comm);
    }

    // GET BY PATIENT
    public List<CommunicationDto> getByPatientId(Long patientId) {
        log.debug("Getting FHIR Communications for patient: {}", patientId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Communication.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                .returnBundle(Bundle.class)
                .execute();

        List<Communication> comms = fhirClientService.extractResources(bundle, Communication.class);
        return comms.stream().map(this::fromFhirCommunication).collect(Collectors.toList());
    }

    // GET CONVERSATION (between patient and provider)
    public List<CommunicationDto> getConversation(Long patientId, Long providerId) {
        log.debug("Getting conversation between patient: {} and provider: {}", patientId, providerId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Communication.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                .returnBundle(Bundle.class)
                .execute();

        List<Communication> comms = fhirClientService.extractResources(bundle, Communication.class);
        
        // Filter by provider involvement (sender or recipient)
        return comms.stream()
                .filter(c -> involvesProvider(c, providerId))
                .map(this::fromFhirCommunication)
                .sorted(Comparator.comparing(CommunicationDto::getSentDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    // GET ALL
    public List<CommunicationDto> searchAll() {
        log.debug("Getting all FHIR Communications");

        Bundle bundle = fhirClientService.search(Communication.class, getPracticeId());
        List<Communication> comms = fhirClientService.extractResources(bundle, Communication.class);

        return comms.stream().map(this::fromFhirCommunication).collect(Collectors.toList());
    }

    // UPDATE
    public CommunicationDto update(String fhirId, CommunicationDto dto) {
        log.debug("Updating FHIR Communication: {}", fhirId);

        Communication comm = toFhirCommunication(dto);
        comm.setId(fhirId);
        fhirClientService.update(comm, getPracticeId());

        dto.setId(parseFhirIdToLong(fhirId));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        return dto;
    }

    // MARK AS READ
    public CommunicationDto markAsRead(String fhirId, String readBy) {
        log.debug("Marking FHIR Communication as read: {}", fhirId);

        Communication comm = fhirClientService.read(Communication.class, fhirId, getPracticeId());
        
        // Add read extensions
        String now = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        comm.addExtension(new Extension(EXT_READ_AT, new StringType(now)));
        comm.addExtension(new Extension(EXT_READ_BY, new StringType(readBy)));
        
        fhirClientService.update(comm, getPracticeId());
        return fromFhirCommunication(comm);
    }

    // SET STATUS
    public CommunicationDto setStatus(String fhirId, CommunicationStatus status) {
        log.debug("Setting status for FHIR Communication: {} to {}", fhirId, status);

        Communication comm = fhirClientService.read(Communication.class, fhirId, getPracticeId());
        comm.setStatus(mapToFhirStatus(status));
        fhirClientService.update(comm, getPracticeId());

        return fromFhirCommunication(comm);
    }

    // DELETE
    public void delete(String fhirId) {
        log.debug("Deleting FHIR Communication: {}", fhirId);
        fhirClientService.delete(Communication.class, fhirId, getPracticeId());
    }

    // -------- FHIR Mapping --------

    private Communication toFhirCommunication(CommunicationDto dto) {
        Communication c = new Communication();

        // Status
        c.setStatus(mapToFhirStatus(dto.getStatus()));

        // Category
        if (dto.getCategory() != null) {
            c.addCategory().setText(dto.getCategory());
        }

        // Subject (Patient)
        if (dto.getPatientId() != null) {
            c.setSubject(new Reference("Patient/" + dto.getPatientId()));
        }

        // Sender
        String fromType = dto.getFromType();
        if (fromType == null) fromType = "patient";
        
        if ("provider".equals(fromType) && dto.getProviderId() != null) {
            c.setSender(new Reference("Practitioner/" + dto.getProviderId()));
        } else if (dto.getPatientId() != null) {
            c.setSender(new Reference("Patient/" + dto.getPatientId()));
        }

        // Recipient
        if ("provider".equals(fromType) && dto.getPatientId() != null) {
            c.addRecipient(new Reference("Patient/" + dto.getPatientId()));
        } else if (dto.getProviderId() != null) {
            c.addRecipient(new Reference("Practitioner/" + dto.getProviderId()));
        }

        // Payload
        if (dto.getPayload() != null) {
            c.addPayload().setContent(new StringType(dto.getPayload()));
        }

        // Sent date
        if (dto.getSentDate() != null) {
            c.setSent(Date.from(parseIsoInstant(dto.getSentDate())));
        } else {
            c.setSent(new Date());
        }

        // Note (subject line stored as note)
        if (dto.getSubject() != null) {
            c.addNote().setText(dto.getSubject());
        }

        // In response to
        if (dto.getInResponseTo() != null) {
            c.addInResponseTo(new Reference("Communication/" + dto.getInResponseTo()));
        }

        // Extensions for custom fields
        if (dto.getMessageType() != null) {
            c.addExtension(new Extension(EXT_MESSAGE_TYPE, new StringType(dto.getMessageType())));
        }
        if (dto.getFromType() != null) {
            c.addExtension(new Extension(EXT_FROM_TYPE, new StringType(dto.getFromType())));
        }
        if (dto.getFromName() != null) {
            c.addExtension(new Extension(EXT_FROM_NAME, new StringType(dto.getFromName())));
        }
        if (dto.getReadAt() != null) {
            c.addExtension(new Extension(EXT_READ_AT, new StringType(dto.getReadAt())));
        }
        if (dto.getReadBy() != null) {
            c.addExtension(new Extension(EXT_READ_BY, new StringType(dto.getReadBy())));
        }

        return c;
    }

    private CommunicationDto fromFhirCommunication(Communication c) {
        CommunicationDto dto = new CommunicationDto();
        String fhirId = c.getIdElement().getIdPart();
        dto.setId(parseFhirIdToLong(fhirId));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);

        // Status
        dto.setStatus(mapFromFhirStatus(c.getStatus()));

        // Category
        if (c.hasCategory()) {
            dto.setCategory(c.getCategoryFirstRep().getText());
        }

        // Subject -> patientId
        if (c.hasSubject() && c.getSubject().hasReference()) {
            String ref = c.getSubject().getReference();
            if (ref.startsWith("Patient/")) {
                try {
                    dto.setPatientId(Long.parseLong(ref.substring("Patient/".length())));
                } catch (NumberFormatException ignored) {}
            }
        }

        // Sender -> fromId, fromType
        if (c.hasSender() && c.getSender().hasReference()) {
            String ref = c.getSender().getReference();
            if (ref.startsWith("Practitioner/")) {
                dto.setFromType("provider");
                try {
                    dto.setProviderId(Long.parseLong(ref.substring("Practitioner/".length())));
                    dto.setFromId(dto.getProviderId());
                } catch (NumberFormatException ignored) {}
            } else if (ref.startsWith("Patient/")) {
                dto.setFromType("patient");
                try {
                    dto.setFromId(Long.parseLong(ref.substring("Patient/".length())));
                } catch (NumberFormatException ignored) {}
            }
        }

        // Recipients -> providerId (if patient sent) or patientId (if provider sent)
        if (c.hasRecipient()) {
            List<Long> toIds = new ArrayList<>();
            for (Reference recipient : c.getRecipient()) {
                if (recipient.hasReference()) {
                    String ref = recipient.getReference();
                    if (ref.startsWith("Practitioner/")) {
                        try {
                            Long id = Long.parseLong(ref.substring("Practitioner/".length()));
                            toIds.add(id);
                            if (dto.getProviderId() == null) dto.setProviderId(id);
                        } catch (NumberFormatException ignored) {}
                    } else if (ref.startsWith("Patient/")) {
                        try {
                            toIds.add(Long.parseLong(ref.substring("Patient/".length())));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
            dto.setToIds(toIds);
        }

        // Payload
        if (c.hasPayload()) {
            var payload = c.getPayloadFirstRep().getContent();
            if (payload instanceof StringType) {
                dto.setPayload(((StringType) payload).getValue());
            }
        }

        // Sent date
        if (c.hasSent()) {
            dto.setSentDate(ZonedDateTime.ofInstant(c.getSent().toInstant(), ZoneId.systemDefault())
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }

        // Note -> subject
        if (c.hasNote()) {
            dto.setSubject(c.getNoteFirstRep().getText());
        }

        // In response to
        if (c.hasInResponseTo()) {
            String ref = c.getInResponseToFirstRep().getReference();
            if (ref != null && ref.startsWith("Communication/")) {
                dto.setInResponseTo(ref.substring("Communication/".length()));
            }
        }

        // Extensions
        Extension msgType = c.getExtensionByUrl(EXT_MESSAGE_TYPE);
        if (msgType != null && msgType.getValue() instanceof StringType) {
            dto.setMessageType(((StringType) msgType.getValue()).getValue());
        }
        Extension fromTypeExt = c.getExtensionByUrl(EXT_FROM_TYPE);
        if (fromTypeExt != null && fromTypeExt.getValue() instanceof StringType) {
            dto.setFromType(((StringType) fromTypeExt.getValue()).getValue());
        }
        Extension fromNameExt = c.getExtensionByUrl(EXT_FROM_NAME);
        if (fromNameExt != null && fromNameExt.getValue() instanceof StringType) {
            dto.setFromName(((StringType) fromNameExt.getValue()).getValue());
        }
        Extension readAtExt = c.getExtensionByUrl(EXT_READ_AT);
        if (readAtExt != null && readAtExt.getValue() instanceof StringType) {
            dto.setReadAt(((StringType) readAtExt.getValue()).getValue());
        }
        Extension readByExt = c.getExtensionByUrl(EXT_READ_BY);
        if (readByExt != null && readByExt.getValue() instanceof StringType) {
            dto.setReadBy(((StringType) readByExt.getValue()).getValue());
        }

        // Infer messageType if not set
        if (dto.getMessageType() == null && dto.getFromType() != null) {
            dto.setMessageType("provider".equals(dto.getFromType()) ? "provider_to_patient" : "patient_to_provider");
        }

        // Populate toNames from toIds
        if (dto.getToIds() != null && !dto.getToIds().isEmpty()) {
            List<String> toNames = new ArrayList<>();
            for (Long toId : dto.getToIds()) {
                try {
                    var patient = fhirClientService.read(Patient.class, String.valueOf(toId), getPracticeId());
                    if (patient.hasName() && !patient.getName().isEmpty()) {
                        HumanName name = patient.getNameFirstRep();
                        String fullName = (name.hasGiven() ? name.getGiven().get(0).getValue() + " " : "") + 
                                         (name.hasFamily() ? name.getFamily() : "");
                        toNames.add(fullName.trim());
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch patient name for ID: {}", toId);
                }
            }
            dto.setToNames(toNames);
        }

        return dto;
    }

    // -------- Helpers --------

    private boolean involvesProvider(Communication c, Long providerId) {
        String provRef = "Practitioner/" + providerId;
        
        if (c.hasSender() && provRef.equals(c.getSender().getReference())) {
            return true;
        }
        if (c.hasRecipient()) {
            for (Reference r : c.getRecipient()) {
                if (provRef.equals(r.getReference())) {
                    return true;
                }
            }
        }
        return false;
    }

    private Instant parseIsoInstant(String iso) {
        try {
            return Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(iso));
        } catch (Exception ignore) {
            return Instant.parse(iso);
        }
    }

    private Communication.CommunicationStatus mapToFhirStatus(CommunicationStatus status) {
        if (status == null) return Communication.CommunicationStatus.COMPLETED;
        return switch (status) {
            case IN_PROGRESS -> Communication.CommunicationStatus.INPROGRESS;
            case SENT -> Communication.CommunicationStatus.COMPLETED;
            case RECEIVED -> Communication.CommunicationStatus.COMPLETED;
            case COMPLETED -> Communication.CommunicationStatus.COMPLETED;
            case ENTERED_IN_ERROR -> Communication.CommunicationStatus.ENTEREDINERROR;
            case ARCHIVED -> Communication.CommunicationStatus.COMPLETED;
            case PREPARATION, NOT_DONE, ON_HOLD, STOPPED, UNKNOWN -> Communication.CommunicationStatus.COMPLETED;
        };
    }

    private CommunicationStatus mapFromFhirStatus(Communication.CommunicationStatus status) {
        if (status == null) return CommunicationStatus.SENT;
        return switch (status) {
            case PREPARATION -> CommunicationStatus.IN_PROGRESS;
            case INPROGRESS -> CommunicationStatus.IN_PROGRESS;
            case COMPLETED -> CommunicationStatus.COMPLETED;
            case ENTEREDINERROR -> CommunicationStatus.ENTERED_IN_ERROR;
            default -> CommunicationStatus.SENT;
        };
    }

    private Long parseFhirIdToLong(String fhirId) {
        if (fhirId == null) return null;
        try {
            return Long.parseLong(fhirId);
        } catch (NumberFormatException e) {
            log.warn("FHIR ID '{}' is not numeric, using hashCode", fhirId);
            return (long) Math.abs(fhirId.hashCode());
        }
    }
}
