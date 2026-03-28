package org.ciyex.ehr.service;

import org.ciyex.ehr.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * FHIR-only Insurance Card Upload Service.
 * Uses FHIR DocumentReference + Binary resources for storing insurance card images.
 * No S3 storage - all data stored in FHIR server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InsuranceCardUploadService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "application/pdf"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    private static final String DOC_TYPE_SYSTEM = "http://ciyex.com/fhir/document-type";
    private static final String DOC_TYPE_CODE = "insurance-card";
    private static final String EXT_COVERAGE_ID = "http://ciyex.com/fhir/StructureDefinition/coverage-id";
    private static final String EXT_CARD_SIDE = "http://ciyex.com/fhir/StructureDefinition/card-side";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    /**
     * Upload insurance card image to FHIR server and return the FHIR ID
     */
    public String uploadCard(MultipartFile file, Long coverageId, String side) throws IOException {
        validateFile(file);

        log.debug("Uploading insurance card {} for coverage {}", side, coverageId);

        // Create DocumentReference with embedded data
        DocumentReference doc = new DocumentReference();
        doc.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);

        // Type to identify as insurance card
        CodeableConcept type = new CodeableConcept();
        type.addCoding().setSystem(DOC_TYPE_SYSTEM).setCode(DOC_TYPE_CODE).setDisplay("Insurance Card");
        doc.setType(type);

        // Description
        doc.setDescription(String.format("Insurance card %s for coverage %d", side, coverageId));

        // Coverage ID extension
        doc.addExtension(new Extension(EXT_COVERAGE_ID, new StringType(coverageId.toString())));

        // Card side extension (front/back)
        doc.addExtension(new Extension(EXT_CARD_SIDE, new StringType(side)));

        // Content with embedded data
        DocumentReference.DocumentReferenceContentComponent content = doc.addContent();
        Attachment attachment = new Attachment();
        attachment.setContentType(file.getContentType());
        attachment.setData(file.getBytes());
        attachment.setTitle(file.getOriginalFilename());
        attachment.setSize((int) file.getSize());
        content.setAttachment(attachment);

        // Create in FHIR
        var outcome = fhirClientService.create(doc, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        log.info("Uploaded insurance card {} for coverage {} with FHIR ID: {}", side, coverageId, fhirId);
        return fhirId;
    }

    /**
     * Get insurance card image data by FHIR ID
     */
    public byte[] getCardData(String fhirId) {
        log.debug("Getting insurance card data: {}", fhirId);
        DocumentReference doc = fhirClientService.read(DocumentReference.class, fhirId, getPracticeId());

        if (doc.hasContent() && !doc.getContent().isEmpty()) {
            DocumentReference.DocumentReferenceContentComponent content = doc.getContent().get(0);
            if (content.hasAttachment() && content.getAttachment().hasData()) {
                return content.getAttachment().getData();
            }
        }
        return new byte[0];
    }

    /**
     * Get insurance card content type by FHIR ID
     */
    public String getCardContentType(String fhirId) {
        DocumentReference doc = fhirClientService.read(DocumentReference.class, fhirId, getPracticeId());

        if (doc.hasContent() && !doc.getContent().isEmpty()) {
            DocumentReference.DocumentReferenceContentComponent content = doc.getContent().get(0);
            if (content.hasAttachment()) {
                return content.getAttachment().getContentType();
            }
        }
        return "application/octet-stream";
    }

    /**
     * Delete insurance card from FHIR
     */
    public void deleteCard(String fhirId) {
        log.debug("Deleting insurance card: {}", fhirId);
        fhirClientService.delete(DocumentReference.class, fhirId, getPracticeId());
    }

    /**
     * Get all insurance cards for a coverage
     */
    public List<String> getCardsForCoverage(Long coverageId) {
        log.debug("Getting insurance cards for coverage: {}", coverageId);
        Bundle bundle = fhirClientService.search(DocumentReference.class, getPracticeId());

        return fhirClientService.extractResources(bundle, DocumentReference.class).stream()
                .filter(this::isInsuranceCard)
                .filter(doc -> coverageId.equals(getCoverageId(doc)))
                .map(doc -> doc.getIdElement().getIdPart())
                .toList();
    }

    private void validateFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IOException("File size exceeds maximum allowed size of 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IOException("Invalid file type. Only PNG, JPEG, and PDF files are allowed");
        }
    }

    private boolean isInsuranceCard(DocumentReference doc) {
        if (!doc.hasType()) return false;
        return doc.getType().getCoding().stream()
                .anyMatch(c -> DOC_TYPE_SYSTEM.equals(c.getSystem()) && DOC_TYPE_CODE.equals(c.getCode()));
    }

    private Long getCoverageId(DocumentReference doc) {
        Extension ext = doc.getExtensionByUrl(EXT_COVERAGE_ID);
        if (ext != null && ext.getValue() instanceof StringType) {
            try {
                return Long.parseLong(((StringType) ext.getValue()).getValue());
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
