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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientEducationService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final String EXT_SUMMARY = "http://ciyex.com/fhir/StructureDefinition/summary";
    private static final String EXT_LANGUAGE = "http://ciyex.com/fhir/StructureDefinition/language";
    private static final String EXT_READING_LEVEL = "http://ciyex.com/fhir/StructureDefinition/reading-level";
    private static final String CATEGORY_CODE = "patient-education";

    private String getPracticeId() {
        String practiceId = practiceContextService.getPracticeId();
        return (practiceId != null && !practiceId.isEmpty()) ? practiceId : "";
    }

    public PatientEducationDto create(PatientEducationDto dto) {
        validateMandatoryFields(dto);
        
        DocumentReference dr = new DocumentReference();
        dr.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);
        dr.addCategory().addCoding()
                .setSystem("http://ciyex.com/fhir/CodeSystem/document-category")
                .setCode(CATEGORY_CODE)
                .setDisplay("Patient Education");

        if (dto.getCategory() != null) dr.getType().setText(dto.getCategory());
        if (dto.getTitle() != null) dr.setDescription(dto.getTitle());
        
        if (dto.getContent() != null) {
            Attachment att = new Attachment();
            att.setTitle(dto.getTitle());
            att.setContentType("text/plain");
            att.setData(dto.getContent().getBytes());
            dr.addContent().setAttachment(att);
        }

        if (dto.getSummary() != null) dr.addExtension(new Extension(EXT_SUMMARY, new StringType(dto.getSummary())));
        if (dto.getLanguage() != null) dr.addExtension(new Extension(EXT_LANGUAGE, new StringType(dto.getLanguage())));
        if (dto.getReadingLevel() != null) dr.addExtension(new Extension(EXT_READING_LEVEL, new StringType(dto.getReadingLevel())));

        var outcome = fhirClientService.create(dr, getPracticeId());
        String fhirId = outcome.getId().getIdPart();
        
        DocumentReference created = fhirClientService.read(DocumentReference.class, fhirId, getPracticeId());
        return fromFhir(created);
    }

    public PatientEducationDto getById(String fhirId) {
        DocumentReference dr = fhirClientService.read(DocumentReference.class, fhirId, getPracticeId());
        return fromFhir(dr);
    }

    public Page<PatientEducationDto> getAll(Pageable pageable) {
        try {
            Bundle bundle = fhirClientService.search(DocumentReference.class, getPracticeId());
            List<DocumentReference> allDocs = fhirClientService.extractResources(bundle, DocumentReference.class);
            
            List<PatientEducationDto> filtered = allDocs.stream()
                    .filter(dr -> dr.hasCategory() && dr.getCategory().stream()
                            .flatMap(cc -> cc.getCoding().stream())
                            .anyMatch(c -> CATEGORY_CODE.equals(c.getCode())))
                    .map(this::fromFhir)
                    .collect(Collectors.toList());

            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), filtered.size());
            List<PatientEducationDto> pageContent = (start < filtered.size()) ? filtered.subList(start, end) : new ArrayList<>();
            
            return new PageImpl<>(pageContent, pageable, filtered.size());
        } catch (Exception e) {
            log.error("Error fetching patient education", e);
            return new PageImpl<>(new ArrayList<>(), pageable, 0);
        }
    }

    public PatientEducationDto update(String fhirId, PatientEducationDto dto) {
        DocumentReference dr = new DocumentReference();
        dr.setId(fhirId);
        dr.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);
        dr.addCategory().addCoding()
                .setSystem("http://ciyex.com/fhir/CodeSystem/document-category")
                .setCode(CATEGORY_CODE)
                .setDisplay("Patient Education");

        if (dto.getCategory() != null) dr.getType().setText(dto.getCategory());
        if (dto.getTitle() != null) dr.setDescription(dto.getTitle());
        
        if (dto.getContent() != null) {
            Attachment att = new Attachment();
            att.setTitle(dto.getTitle());
            att.setContentType("text/plain");
            att.setData(dto.getContent().getBytes());
            dr.addContent().setAttachment(att);
        }

        if (dto.getSummary() != null) dr.addExtension(new Extension(EXT_SUMMARY, new StringType(dto.getSummary())));
        if (dto.getLanguage() != null) dr.addExtension(new Extension(EXT_LANGUAGE, new StringType(dto.getLanguage())));
        if (dto.getReadingLevel() != null) dr.addExtension(new Extension(EXT_READING_LEVEL, new StringType(dto.getReadingLevel())));

        fhirClientService.update(dr, getPracticeId());
        DocumentReference updated = fhirClientService.read(DocumentReference.class, fhirId, getPracticeId());
        return fromFhir(updated);
    }

    public void delete(String fhirId) {
        fhirClientService.delete(DocumentReference.class, fhirId, getPracticeId());
    }

    private PatientEducationDto fromFhir(DocumentReference dr) {
        PatientEducationDto dto = new PatientEducationDto();
        String fhirId = dr.getIdElement().getIdPart();
        dto.setFhirId(fhirId);
        
        try {
            dto.setId(Long.valueOf(fhirId));
        } catch (NumberFormatException e) {
            dto.setId((long) Math.abs(fhirId.hashCode()));
        }
        
        PatientEducationDto.Audit audit = new PatientEducationDto.Audit();
        if (dr.getMeta() != null && dr.getMeta().hasLastUpdated()) {
            String ts = dr.getMeta().getLastUpdated().toInstant().toString();
            audit.setCreatedDate(ts);
            audit.setLastModifiedDate(ts);
        } else {
            String ts = java.time.Instant.now().toString();
            audit.setCreatedDate(ts);
            audit.setLastModifiedDate(ts);
        }
        dto.setAudit(audit);

        if (dr.hasType() && dr.getType().hasText()) dto.setCategory(dr.getType().getText());
        if (dr.hasDescription()) dto.setTitle(dr.getDescription());
        
        if (dr.hasContent()) {
            Attachment att = dr.getContentFirstRep().getAttachment();
            if (att.hasData()) dto.setContent(new String(att.getData()));
            if (att.hasTitle() && dto.getTitle() == null) dto.setTitle(att.getTitle());
        }

        dto.setSummary(getExt(dr, EXT_SUMMARY));
        dto.setLanguage(getExt(dr, EXT_LANGUAGE));
        dto.setReadingLevel(getExt(dr, EXT_READING_LEVEL));

        return dto;
    }

    private String getExt(DocumentReference dr, String url) {
        Extension ext = dr.getExtensionByUrl(url);
        return (ext != null && ext.getValue() instanceof StringType) ? ((StringType) ext.getValue()).getValue() : null;
    }

    private void validateMandatoryFields(PatientEducationDto dto) {
        List<String> missing = new ArrayList<>();
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) missing.add("title");
        if (dto.getCategory() == null || dto.getCategory().trim().isEmpty()) missing.add("category");
        if (dto.getLanguage() == null || dto.getLanguage().trim().isEmpty()) missing.add("language");
        if (dto.getReadingLevel() == null || dto.getReadingLevel().trim().isEmpty()) missing.add("readingLevel");
        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) missing.add("content");
        
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Missing mandatory fields: " + String.join(", ", missing));
        }
    }
}
