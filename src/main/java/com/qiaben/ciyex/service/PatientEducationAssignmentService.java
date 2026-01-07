package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import com.qiaben.ciyex.dto.PatientEducationAssignmentDto;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-only PatientEducationAssignment Service.
 * Uses FHIR ServiceRequest resource directly via FhirClientService.
 * No local database storage - all data stored in FHIR server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PatientEducationAssignmentService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Extension URLs
    private static final String EXT_PATIENT_NAME = "http://ciyex.com/fhir/StructureDefinition/patient-name";
    private static final String EXT_NOTES = "http://ciyex.com/fhir/StructureDefinition/notes";
    private static final String EXT_DELIVERED = "http://ciyex.com/fhir/StructureDefinition/delivered";
    private static final String EXT_ASSIGNED_DATE = "http://ciyex.com/fhir/StructureDefinition/assigned-date";
    private static final String EXT_TOPIC_ID = "http://ciyex.com/fhir/StructureDefinition/topic-id";
    private static final String EXT_TOPIC_TITLE = "http://ciyex.com/fhir/StructureDefinition/topic-title";
    private static final String EXT_TOPIC_SUMMARY = "http://ciyex.com/fhir/StructureDefinition/topic-summary";
    private static final String EXT_TOPIC_CATEGORY = "http://ciyex.com/fhir/StructureDefinition/topic-category";
    private static final String EXT_TOPIC_LANGUAGE = "http://ciyex.com/fhir/StructureDefinition/topic-language";
    private static final String EXT_TOPIC_READING_LEVEL = "http://ciyex.com/fhir/StructureDefinition/topic-reading-level";
    private static final String EXT_TOPIC_CONTENT = "http://ciyex.com/fhir/StructureDefinition/topic-content";
    private static final String EXT_TOPIC_FHIR_ID = "http://ciyex.com/fhir/StructureDefinition/topic-fhir-id";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // ASSIGN (CREATE)
    public PatientEducationAssignmentDto assign(String educationId, PatientEducationAssignmentDto dto) {
        log.debug("Creating FHIR ServiceRequest (education assignment) for patient: {}", dto.getPatientId());

        dto.setDelivered(true);
        dto.setAssignedDate(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

        ServiceRequest sr = toFhirServiceRequest(dto);
        var outcome = fhirClientService.create(sr, getPracticeId());
        String fhirId = outcome.getId().getIdPart();
        dto.setId(Long.parseLong(fhirId));
        dto.setFhirId(fhirId);

        // Set audit information
        PatientEducationAssignmentDto.Audit audit = new PatientEducationAssignmentDto.Audit();
        audit.setCreatedDate(java.time.LocalDateTime.now().toString());
        audit.setLastModifiedDate(java.time.LocalDateTime.now().toString());
        dto.setAudit(audit);

        log.info("Created FHIR ServiceRequest (education assignment) with id: {}", fhirId);

        return dto;
    }

    // GET BY PATIENT
    public List<PatientEducationAssignmentDto> getByPatient(Long patientId) {
        log.debug("Getting FHIR ServiceRequests (education assignments) for patient: {}", patientId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(ServiceRequest.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                .returnBundle(Bundle.class)
                .execute();

        List<ServiceRequest> requests = fhirClientService.extractResources(bundle, ServiceRequest.class);
        return requests.stream()
                .filter(this::isEducationAssignment)
                .map(this::fromFhirServiceRequest)
                .collect(Collectors.toList());
    }

    // GET ALL
    public List<PatientEducationAssignmentDto> getAll() {
        log.debug("Getting all FHIR ServiceRequests (education assignments)");

        Bundle bundle = fhirClientService.search(ServiceRequest.class, getPracticeId());
        List<ServiceRequest> requests = fhirClientService.extractResources(bundle, ServiceRequest.class);

        return requests.stream()
                .filter(this::isEducationAssignment)
                .map(this::fromFhirServiceRequest)
                .collect(Collectors.toList());
    }

    // DELETE
    public void delete(String fhirId) {
        log.debug("Deleting FHIR ServiceRequest (education assignment): {}", fhirId);
        fhirClientService.delete(ServiceRequest.class, fhirId, getPracticeId());
    }

    // MARK DELIVERED
    public PatientEducationAssignmentDto markDelivered(String fhirId) {
        log.debug("Marking FHIR ServiceRequest (education assignment) as delivered: {}", fhirId);

        ServiceRequest sr = fhirClientService.read(ServiceRequest.class, fhirId, getPracticeId());

        // Update delivered extension
        sr.getExtension().removeIf(e -> EXT_DELIVERED.equals(e.getUrl()));
        sr.addExtension(new Extension(EXT_DELIVERED, new BooleanType(true)));
        sr.setStatus(ServiceRequest.ServiceRequestStatus.COMPLETED);

        fhirClientService.update(sr, getPracticeId());
        return fromFhirServiceRequest(sr);
    }

    // COUNT
    public long count() {
        return getAll().size();
    }

    // -------- FHIR Mapping --------

    private ServiceRequest toFhirServiceRequest(PatientEducationAssignmentDto dto) {
        ServiceRequest sr = new ServiceRequest();
        sr.setStatus(dto.isDelivered() ? ServiceRequest.ServiceRequestStatus.COMPLETED : ServiceRequest.ServiceRequestStatus.ACTIVE);
        sr.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);

        // Category = education assignment
        sr.addCategory().addCoding()
                .setSystem("http://ciyex.com/fhir/CodeSystem/service-request-category")
                .setCode("education-assignment")
                .setDisplay("Patient Education Assignment");

        // Subject (Patient)
        if (dto.getPatientId() != null) {
            sr.setSubject(new Reference("Patient/" + dto.getPatientId()));
        }

        // Extensions
        addStringExtension(sr, EXT_PATIENT_NAME, dto.getPatientName());
        addStringExtension(sr, EXT_NOTES, dto.getNotes());
        addStringExtension(sr, EXT_ASSIGNED_DATE, dto.getAssignedDate());
        sr.addExtension(new Extension(EXT_DELIVERED, new BooleanType(dto.isDelivered())));

        // Topic extensions
        if (dto.getTopic() != null) {
            PatientEducationAssignmentDto.TopicDto topic = dto.getTopic();
            if (topic.getId() != null) {
                sr.addExtension(new Extension(EXT_TOPIC_ID, new StringType(topic.getId().toString())));
            }
            addStringExtension(sr, EXT_TOPIC_TITLE, topic.getTitle());
            addStringExtension(sr, EXT_TOPIC_SUMMARY, topic.getSummary());
            addStringExtension(sr, EXT_TOPIC_CATEGORY, topic.getCategory());
            addStringExtension(sr, EXT_TOPIC_LANGUAGE, topic.getLanguage());
            addStringExtension(sr, EXT_TOPIC_READING_LEVEL, topic.getReadingLevel());
            addStringExtension(sr, EXT_TOPIC_CONTENT, topic.getContent());
            addStringExtension(sr, EXT_TOPIC_FHIR_ID, topic.getFhirId());
        }

        return sr;
    }

    private PatientEducationAssignmentDto fromFhirServiceRequest(ServiceRequest sr) {
        PatientEducationAssignmentDto dto = new PatientEducationAssignmentDto();
        dto.setFhirId(sr.getIdElement().getIdPart());
        dto.setId(Long.parseLong(sr.getIdElement().getIdPart()));

        // Subject -> patientId
        if (sr.hasSubject() && sr.getSubject().hasReference()) {
            String ref = sr.getSubject().getReference();
            if (ref.startsWith("Patient/")) {
                try {
                    dto.setPatientId(Long.parseLong(ref.substring("Patient/".length())));
                } catch (NumberFormatException ignored) {}
            }
        }

        // Extensions
        dto.setPatientName(getExtensionString(sr, EXT_PATIENT_NAME));
        dto.setNotes(getExtensionString(sr, EXT_NOTES));
        dto.setAssignedDate(getExtensionString(sr, EXT_ASSIGNED_DATE));

        Extension deliveredExt = sr.getExtensionByUrl(EXT_DELIVERED);
        if (deliveredExt != null && deliveredExt.getValue() instanceof BooleanType) {
            dto.setDelivered(((BooleanType) deliveredExt.getValue()).booleanValue());
        }

        // Topic
        PatientEducationAssignmentDto.TopicDto topic = new PatientEducationAssignmentDto.TopicDto();
        String topicIdStr = getExtensionString(sr, EXT_TOPIC_ID);
        if (topicIdStr != null) {
            try {
                topic.setId(Long.parseLong(topicIdStr));
            } catch (NumberFormatException ignored) {}
        }
        topic.setTitle(getExtensionString(sr, EXT_TOPIC_TITLE));
        topic.setSummary(getExtensionString(sr, EXT_TOPIC_SUMMARY));
        topic.setCategory(getExtensionString(sr, EXT_TOPIC_CATEGORY));
        topic.setLanguage(getExtensionString(sr, EXT_TOPIC_LANGUAGE));
        topic.setReadingLevel(getExtensionString(sr, EXT_TOPIC_READING_LEVEL));
        topic.setContent(getExtensionString(sr, EXT_TOPIC_CONTENT));
        topic.setFhirId(getExtensionString(sr, EXT_TOPIC_FHIR_ID));
        dto.setTopic(topic);

        // Set audit information
        PatientEducationAssignmentDto.Audit audit = new PatientEducationAssignmentDto.Audit();
        audit.setCreatedDate(java.time.LocalDateTime.now().toString());
        audit.setLastModifiedDate(java.time.LocalDateTime.now().toString());
        dto.setAudit(audit);

        return dto;
    }

    // -------- Helpers --------

    private boolean isEducationAssignment(ServiceRequest sr) {
        if (!sr.hasCategory()) return false;
        return sr.getCategory().stream()
                .flatMap(cc -> cc.getCoding().stream())
                .anyMatch(c -> "education-assignment".equals(c.getCode()));
    }

    private void addStringExtension(ServiceRequest sr, String url, String value) {
        if (value != null) {
            sr.addExtension(new Extension(url, new StringType(value)));
        }
    }

    private String getExtensionString(ServiceRequest sr, String url) {
        Extension ext = sr.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) {
            return ((StringType) ext.getValue()).getValue();
        }
        return null;
    }
}
