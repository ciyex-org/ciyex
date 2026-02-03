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
import java.util.ArrayList;
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
    private final PatientEducationService patientEducationService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Extension URLs
    private static final String EXT_PATIENT_NAME = "http://ciyex.com/fhir/StructureDefinition/patient-name";
    private static final String EXT_NOTES = "http://ciyex.com/fhir/StructureDefinition/notes";
    private static final String EXT_DELIVERED = "http://ciyex.com/fhir/StructureDefinition/delivered";
    private static final String EXT_ASSIGNED_DATE = "http://ciyex.com/fhir/StructureDefinition/assigned-date";
    private static final String EXT_ASSIGNED_BY = "http://ciyex.com/fhir/StructureDefinition/assigned-by";
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

        try {
            // Fetch education topic from MySQL if not provided
            if (dto.getTopic() == null && educationId != null) {
                var education = patientEducationService.getById(educationId);
                PatientEducationAssignmentDto.TopicDto topic = new PatientEducationAssignmentDto.TopicDto();
                topic.setId(education.getId());
                topic.setTitle(education.getTitle());
                topic.setSummary(education.getSummary());
                topic.setCategory(education.getCategory());
                topic.setLanguage(education.getLanguage());
                topic.setReadingLevel(education.getReadingLevel());
                topic.setContent(education.getContent());
                topic.setFhirId(education.getFhirId());
                dto.setTopic(topic);
            }
            
            // Set assignedBy from authenticated user if not provided
            if (dto.getAssignedBy() == null || dto.getAssignedBy().trim().isEmpty()) {
                org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated()) {
                    dto.setAssignedBy(auth.getName());
                }
            }
            
            validateAssignmentDto(dto);
            
            dto.setDelivered(false);
            dto.setAssignedDate(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

            ServiceRequest sr = toFhirServiceRequest(dto);
            var outcome = fhirClientService.create(sr, getPracticeId());
            String fhirId = outcome.getId().getIdPart();
            dto.setId(Long.parseLong(fhirId));
            dto.setFhirId(fhirId);

            // Set audit information
            PatientEducationAssignmentDto.Audit audit = new PatientEducationAssignmentDto.Audit();
            audit.setCreatedDate(LocalDateTime.now().toString());
            audit.setLastModifiedDate(LocalDateTime.now().toString());
            dto.setAudit(audit);

            log.info("Created FHIR ServiceRequest (education assignment) with id: {}", fhirId);

            return dto;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create education assignment: {}", e.getMessage());
            throw new RuntimeException("Failed to create education assignment: " + e.getMessage(), e);
        }
    }

    // GET BY PATIENT
    public List<PatientEducationAssignmentDto> getByPatient(Long patientId) {
        if (patientId == null) {
            throw new IllegalArgumentException("Patient ID cannot be null");
        }
        
        log.debug("Getting FHIR ServiceRequests (education assignments) for patient: {}", patientId);

        try {
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
        } catch (Exception e) {
            log.error("Failed to retrieve assignments for patient {}: {}", patientId, e.getMessage());
            throw new RuntimeException("Failed to retrieve assignments for patient: " + patientId, e);
        }
    }
    
    // GET BY ID
    public PatientEducationAssignmentDto getById(String fhirId) {
        if (fhirId == null || fhirId.trim().isEmpty()) {
            throw new IllegalArgumentException("Assignment ID cannot be null or empty");
        }
        
        log.debug("Getting FHIR ServiceRequest (education assignment): {}", fhirId);
        
        try {
            ServiceRequest sr = fhirClientService.read(ServiceRequest.class, fhirId, getPracticeId());
            if (sr == null) {
                throw new IllegalArgumentException("Assignment not found for assignmentId=" + fhirId);
            }
            return fromFhirServiceRequest(sr);
        } catch (Exception e) {
            log.error("Failed to retrieve assignment with ID {}: {}", fhirId, e.getMessage());
            throw new RuntimeException("Failed to retrieve assignment: Assignment not found for assignmentId=" + fhirId);
        }
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
        if (fhirId == null || fhirId.trim().isEmpty()) {
            throw new IllegalArgumentException("Assignment ID cannot be null or empty");
        }
        
        log.debug("Deleting FHIR ServiceRequest (education assignment): {}", fhirId);
        
        try {
            // Verify assignment exists before deleting
            fhirClientService.read(ServiceRequest.class, fhirId, getPracticeId());
            fhirClientService.delete(ServiceRequest.class, fhirId, getPracticeId());
            log.info("Successfully deleted assignment with ID: {}", fhirId);
        } catch (Exception e) {
            log.error("Failed to delete assignment with ID {}: {}", fhirId, e.getMessage());
            throw new RuntimeException("Failed to delete assignment: Assignment not found for assignmentId=" + fhirId);
        }
    }

    // MARK DELIVERED
    public PatientEducationAssignmentDto markDelivered(String fhirId) {
        if (fhirId == null || fhirId.trim().isEmpty()) {
            throw new IllegalArgumentException("Assignment ID cannot be null or empty");
        }
        
        log.debug("Marking FHIR ServiceRequest (education assignment) as delivered: {}", fhirId);

        try {
            ServiceRequest sr = fhirClientService.read(ServiceRequest.class, fhirId, getPracticeId());
            
            if (sr == null) {
                throw new IllegalArgumentException("Assignment not found for assignmentId=" + fhirId);
            }

            // Update delivered extension
            sr.getExtension().removeIf(e -> EXT_DELIVERED.equals(e.getUrl()));
            sr.addExtension(new Extension(EXT_DELIVERED, new BooleanType(true)));
            sr.setStatus(ServiceRequest.ServiceRequestStatus.COMPLETED);

            fhirClientService.update(sr, getPracticeId());
            
            PatientEducationAssignmentDto result = fromFhirServiceRequest(sr);
            log.info("Successfully marked assignment {} as delivered", fhirId);
            
            return result;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to mark assignment as delivered for ID {}: {}", fhirId, e.getMessage());
            throw new RuntimeException("Failed to mark assignment as delivered: Assignment not found for assignmentId=" + fhirId);
        }
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
        addStringExtension(sr, EXT_ASSIGNED_BY, dto.getAssignedBy());
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
        dto.setAssignedBy(getExtensionString(sr, EXT_ASSIGNED_BY));

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
    
    // -------- Validation --------
    
    private void validateAssignmentDto(PatientEducationAssignmentDto dto) {
        List<String> errors = new ArrayList<>();
        
        if (dto == null) {
            throw new IllegalArgumentException("Assignment data cannot be null");
        }
        
        if (dto.getPatientId() == null) {
            errors.add("Patient ID is mandatory");
        }
        
        if (dto.getTopic() == null) {
            errors.add("Topic is mandatory");
        } else {
            if (dto.getTopic().getTitle() == null || dto.getTopic().getTitle().trim().isEmpty()) {
                errors.add("Topic title is mandatory");
            }
        }
        
        if (!errors.isEmpty()) {
            String errorMessage = "Validation failed: " + String.join(", ", errors);
            log.error("Assignment validation failed: {}", errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
