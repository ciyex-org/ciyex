package org.ciyex.ehr.service;

import org.ciyex.ehr.dto.TemplateDocumentResponse;
import org.ciyex.ehr.dto.TemplateDocumentUpsertRequest;
import org.ciyex.ehr.dto.TemplateContext;
import org.ciyex.ehr.fhir.FhirClientService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * FHIR-only TemplateDocument Service (HTML document templates).
 * Uses FHIR DocumentReference resource for storing HTML templates.
 * No local database storage - all data stored in FHIR server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateDocumentService {

    private final FhirClientService fhirClientService;
    private final ObjectMapper objectMapper;

    private static final String TEMPLATE_TYPE_SYSTEM = "http://ciyex.com/fhir/document-type";
    private static final String TEMPLATE_TYPE_CODE = "html-template";
    private static final String EXT_CONTEXT = "http://ciyex.com/fhir/StructureDefinition/template-context";
    private static final String EXT_NAME = "http://ciyex.com/fhir/StructureDefinition/template-name";
    private static final String EXT_OPTIONS = "http://ciyex.com/fhir/StructureDefinition/template-options";

    // CREATE
    public TemplateDocumentResponse create(TemplateDocumentUpsertRequest req, String orgAlias) {
        log.debug("Creating FHIR DocumentReference (TemplateDocument): {} for org: {}", req.name, orgAlias);

        DocumentReference doc = toFhirDocumentReference(req);
        var outcome = fhirClientService.create(doc, orgAlias);
        String fhirId = outcome.getId().getIdPart();

        log.info("Created FHIR DocumentReference (TemplateDocument) with id={} name='{}' for org: {}", fhirId, req.name, orgAlias);
        return toResponse(fhirId, req);
    }

    // UPDATE
    public TemplateDocumentResponse update(String fhirId, TemplateDocumentUpsertRequest req, String orgAlias) {
        log.debug("Updating template document: {} for org: {}", fhirId, orgAlias);

        DocumentReference doc = toFhirDocumentReference(req);
        doc.setId(fhirId);
        fhirClientService.update(doc, orgAlias);

        log.info("Updated template document id={} name='{}' for org: {}", fhirId, req.name, orgAlias);
        return toResponse(fhirId, req);
    }

    // GET ONE
    public TemplateDocumentResponse getOne(String fhirId, String orgAlias) {
        log.debug("Getting template document: {} for org: {}", fhirId, orgAlias);
        DocumentReference doc = fhirClientService.read(DocumentReference.class, fhirId, orgAlias);
        if (doc == null) {
            throw new ResponseStatusException(NOT_FOUND, "Template not found");
        }
        return fromFhirDocumentReference(doc);
    }

    // GET ALL with optional filters
    public List<TemplateDocumentResponse> getAll(TemplateContext context, String q, String orgAlias) {
        log.debug("Getting all template documents for org: {}, context={}, q={}", orgAlias, context, q);
        try {
            Bundle bundle = fhirClientService.search(DocumentReference.class, orgAlias);
            
            return fhirClientService.extractResources(bundle, DocumentReference.class).stream()
                    .filter(this::isHtmlTemplate)
                    .map(this::fromFhirDocumentReference)
                    .filter(r -> context == null || context.equals(r.context))
                    .filter(r -> q == null || q.isBlank() || (r.name != null && r.name.toLowerCase().contains(q.toLowerCase())))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to search templates: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // DELETE
    public void delete(String fhirId, String orgAlias) {
        log.debug("Deleting template document: {} for org: {}", fhirId, orgAlias);
        fhirClientService.delete(DocumentReference.class, fhirId, orgAlias);
        log.info("Deleted template document id={} for org: {}", fhirId, orgAlias);
    }

    // GET HTML RAW
    public String getHtmlRaw(String fhirId, String orgAlias) {
        log.debug("Getting raw HTML for template: {} for org: {}", fhirId, orgAlias);
        DocumentReference doc = fhirClientService.read(DocumentReference.class, fhirId, orgAlias);
        if (doc == null) {
            throw new ResponseStatusException(NOT_FOUND, "Template not found");
        }

        // Extract content from attachment
        if (doc.hasContent() && !doc.getContent().isEmpty()) {
            DocumentReference.DocumentReferenceContentComponent content = doc.getContent().get(0);
            if (content.hasAttachment() && content.getAttachment().hasData()) {
                return new String(content.getAttachment().getData());
            }
        }
        return "";
    }

    // -------- FHIR Mapping --------

    private DocumentReference toFhirDocumentReference(TemplateDocumentUpsertRequest req) {
        DocumentReference doc = new DocumentReference();
        doc.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);

        // Type to identify as HTML template
        CodeableConcept type = new CodeableConcept();
        type.addCoding().setSystem(TEMPLATE_TYPE_SYSTEM).setCode(TEMPLATE_TYPE_CODE).setDisplay("HTML Template");
        doc.setType(type);

        // Context
        if (req.context != null) {
            doc.addExtension(new Extension(EXT_CONTEXT, new StringType(req.context.toString())));
        }

        // Name
        if (req.name != null) {
            doc.addExtension(new Extension(EXT_NAME, new StringType(req.name)));
            doc.setDescription(req.name);
        }

        // Options as JSON
        if (req.options != null && !req.options.isEmpty()) {
            try {
                String optionsJson = objectMapper.writeValueAsString(req.options);
                doc.addExtension(new Extension(EXT_OPTIONS, new StringType(optionsJson)));
            } catch (JsonProcessingException e) {
                log.warn("Could not serialize options: {}", e.getMessage());
            }
        }

        // Content (HTML) as attachment
        if (req.content != null) {
            DocumentReference.DocumentReferenceContentComponent content = doc.addContent();
            Attachment attachment = new Attachment();
            attachment.setContentType("text/html");
            attachment.setData(req.content.getBytes());
            content.setAttachment(attachment);
        }

        return doc;
    }

    private TemplateDocumentResponse fromFhirDocumentReference(DocumentReference doc) {
        TemplateDocumentResponse r = new TemplateDocumentResponse();

        r.id = doc.getIdElement().getIdPart();

        // Context
        Extension contextExt = doc.getExtensionByUrl(EXT_CONTEXT);
        if (contextExt != null && contextExt.getValue() instanceof StringType) {
            try {
                r.context = TemplateContext.from(((StringType) contextExt.getValue()).getValue());
            } catch (Exception ignored) {}
        }

        // Name
        Extension nameExt = doc.getExtensionByUrl(EXT_NAME);
        if (nameExt != null && nameExt.getValue() instanceof StringType) {
            r.name = ((StringType) nameExt.getValue()).getValue();
        } else if (doc.hasDescription()) {
            r.name = doc.getDescription();
        }

        // Options
        Extension optionsExt = doc.getExtensionByUrl(EXT_OPTIONS);
        if (optionsExt != null && optionsExt.getValue() instanceof StringType) {
            try {
                String json = ((StringType) optionsExt.getValue()).getValue();
                r.options = objectMapper.readValue(json, Map.class);
            } catch (Exception e) {
                log.warn("Could not deserialize options: {}", e.getMessage());
            }
        }

        // Content
        if (doc.hasContent() && !doc.getContent().isEmpty()) {
            DocumentReference.DocumentReferenceContentComponent content = doc.getContent().get(0);
            if (content.hasAttachment() && content.getAttachment().hasData()) {
                r.content = new String(content.getAttachment().getData());
            }
        }

        // Timestamps
        r.createdAt = Instant.now();
        r.updatedAt = Instant.now();

        return r;
    }

    private TemplateDocumentResponse toResponse(String fhirId, TemplateDocumentUpsertRequest req) {
        TemplateDocumentResponse r = new TemplateDocumentResponse();
        r.id = fhirId;
        r.context = req.context;
        r.name = req.name;
        r.content = req.content;
        r.options = req.options;
        r.createdAt = Instant.now();
        r.updatedAt = Instant.now();
        return r;
    }

    private boolean isHtmlTemplate(DocumentReference doc) {
        if (!doc.hasType()) return false;
        return doc.getType().getCoding().stream()
                .anyMatch(c -> TEMPLATE_TYPE_SYSTEM.equals(c.getSystem()) && TEMPLATE_TYPE_CODE.equals(c.getCode()));
    }
}
