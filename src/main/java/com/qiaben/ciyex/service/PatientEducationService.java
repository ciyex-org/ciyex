package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.PatientEducationDto;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-only PatientEducation Service.
 * Uses FHIR DocumentReference resource directly via FhirClientService.
 * No local database storage - all data stored in FHIR server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PatientEducationService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    // Extension URLs
    private static final String EXT_SUMMARY = "http://ciyex.com/fhir/StructureDefinition/summary";
    private static final String EXT_LANGUAGE = "http://ciyex.com/fhir/StructureDefinition/language";
    private static final String EXT_READING_LEVEL = "http://ciyex.com/fhir/StructureDefinition/reading-level";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public PatientEducationDto create(PatientEducationDto dto) {
        validateMandatoryFields(dto);

        log.debug("Creating FHIR DocumentReference (patient education): {}", dto.getTitle());

        DocumentReference dr = toFhirDocumentReference(dto);
        var outcome = fhirClientService.create(dr, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setFhirId(fhirId);
        log.info("Created FHIR DocumentReference (patient education) with id: {}", fhirId);

        return dto;
    }

    // GET BY ID
    public PatientEducationDto getById(String fhirId) {
        log.debug("Getting FHIR DocumentReference (patient education): {}", fhirId);
        DocumentReference dr = fhirClientService.read(DocumentReference.class, fhirId, getPracticeId());
        return fromFhirDocumentReference(dr);
    }

    // GET ALL (Paginated)
    public Page<PatientEducationDto> getAll(Pageable pageable) {
        log.debug("Getting all FHIR DocumentReferences (patient education)");

        Bundle bundle = fhirClientService.search(DocumentReference.class, getPracticeId());
        List<DocumentReference> docs = fhirClientService.extractResources(bundle, DocumentReference.class);

        // Filter to patient education documents
        List<PatientEducationDto> all = docs.stream()
                .filter(this::isPatientEducation)
                .map(this::fromFhirDocumentReference)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<PatientEducationDto> pageContent = all.subList(start, end);
        return new PageImpl<>(pageContent, pageable, all.size());
    }

    // UPDATE
    public PatientEducationDto update(String fhirId, PatientEducationDto dto) {
        log.debug("Updating FHIR DocumentReference (patient education): {}", fhirId);

        DocumentReference dr = toFhirDocumentReference(dto);
        dr.setId(fhirId);
        fhirClientService.update(dr, getPracticeId());

        dto.setFhirId(fhirId);
        return dto;
    }

    // DELETE
    public void delete(String fhirId) {
        log.debug("Deleting FHIR DocumentReference (patient education): {}", fhirId);
        fhirClientService.delete(DocumentReference.class, fhirId, getPracticeId());
    }

    // -------- FHIR Mapping --------

    private DocumentReference toFhirDocumentReference(PatientEducationDto dto) {
        DocumentReference dr = new DocumentReference();
        dr.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);

        // Category = patient education
        dr.addCategory().addCoding()
                .setSystem("http://ciyex.com/fhir/CodeSystem/document-category")
                .setCode("patient-education")
                .setDisplay("Patient Education");

        // Type (category from DTO)
        if (dto.getCategory() != null) {
            dr.getType().setText(dto.getCategory());
        }

        // Description (title)
        if (dto.getTitle() != null) {
            dr.setDescription(dto.getTitle());
        }

        // Content (actual education content)
        if (dto.getContent() != null) {
            DocumentReference.DocumentReferenceContentComponent content = dr.addContent();
            Attachment attachment = new Attachment();
            attachment.setTitle(dto.getTitle());
            attachment.setContentType("text/plain");
            attachment.setData(dto.getContent().getBytes());
            content.setAttachment(attachment);
        }

        // Extensions
        if (dto.getSummary() != null) {
            dr.addExtension(new Extension(EXT_SUMMARY, new StringType(dto.getSummary())));
        }
        if (dto.getLanguage() != null) {
            dr.addExtension(new Extension(EXT_LANGUAGE, new StringType(dto.getLanguage())));
        }
        if (dto.getReadingLevel() != null) {
            dr.addExtension(new Extension(EXT_READING_LEVEL, new StringType(dto.getReadingLevel())));
        }

        return dr;
    }

    private PatientEducationDto fromFhirDocumentReference(DocumentReference dr) {
        PatientEducationDto dto = new PatientEducationDto();
        dto.setFhirId(dr.getIdElement().getIdPart());

        // Type -> category
        if (dr.hasType() && dr.getType().hasText()) {
            dto.setCategory(dr.getType().getText());
        }

        // Description -> title
        if (dr.hasDescription()) {
            dto.setTitle(dr.getDescription());
        }

        // Content
        if (dr.hasContent()) {
            Attachment att = dr.getContentFirstRep().getAttachment();
            if (att.hasData()) {
                dto.setContent(new String(att.getData()));
            }
            if (att.hasTitle() && dto.getTitle() == null) {
                dto.setTitle(att.getTitle());
            }
        }

        // Extensions
        dto.setSummary(getExtensionString(dr, EXT_SUMMARY));
        dto.setLanguage(getExtensionString(dr, EXT_LANGUAGE));
        dto.setReadingLevel(getExtensionString(dr, EXT_READING_LEVEL));

        return dto;
    }

    private boolean isPatientEducation(DocumentReference dr) {
        if (!dr.hasCategory()) return false;
        return dr.getCategory().stream()
                .flatMap(cc -> cc.getCoding().stream())
                .anyMatch(c -> "patient-education".equals(c.getCode()));
    }

    private String getExtensionString(DocumentReference dr, String url) {
        Extension ext = dr.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) {
            return ((StringType) ext.getValue()).getValue();
        }
        return null;
    }

    private void validateMandatoryFields(PatientEducationDto dto) {
        StringBuilder errors = new StringBuilder();

        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            errors.append("title, ");
        }
        if (dto.getCategory() == null || dto.getCategory().trim().isEmpty()) {
            errors.append("category, ");
        }
        if (dto.getLanguage() == null || dto.getLanguage().trim().isEmpty()) {
            errors.append("language, ");
        }
        if (dto.getReadingLevel() == null || dto.getReadingLevel().trim().isEmpty()) {
            errors.append("readingLevel, ");
        }
        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            errors.append("content, ");
        }

        if (errors.length() > 0) {
            String missingFields = errors.substring(0, errors.length() - 2);
            throw new IllegalArgumentException("Missing mandatory fields: " + missingFields);
        }
    }
}
