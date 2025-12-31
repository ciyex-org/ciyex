package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.TemplateDocumentResponse;
import com.qiaben.ciyex.dto.TemplateDocumentUpsertRequest;
import com.qiaben.ciyex.entity.TemplateContext;
import com.qiaben.ciyex.fhir.FhirClientService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
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
    private final PracticeContextService practiceContextService;
    private final ObjectMapper objectMapper;

    private static final String TEMPLATE_TYPE_SYSTEM = "http://ciyex.com/fhir/document-type";
    private static final String TEMPLATE_TYPE_CODE = "html-template";
    private static final String EXT_CONTEXT = "http://ciyex.com/fhir/StructureDefinition/template-context";
    private static final String EXT_NAME = "http://ciyex.com/fhir/StructureDefinition/template-name";
    private static final String EXT_OPTIONS = "http://ciyex.com/fhir/StructureDefinition/template-options";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public TemplateDocumentResponse create(TemplateDocumentUpsertRequest req) {
        log.debug("Creating FHIR DocumentReference (TemplateDocument): {}", req.name);

        DocumentReference doc = toFhirDocumentReference(req);
        var outcome = fhirClientService.create(doc, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        log.info("Created FHIR DocumentReference (TemplateDocument) with id={} name='{}'", fhirId, req.name);
        return toResponse(fhirId, req);
    }

    // UPDATE
    public TemplateDocumentResponse update(String fhirId, TemplateDocumentUpsertRequest req) {
        log.debug("Updating template document: {}", fhirId);

        DocumentReference doc = toFhirDocumentReference(req);
        doc.setId(fhirId);
        fhirClientService.update(doc, getPracticeId());

        log.info("Updated template document id={} name='{}'", fhirId, req.name);
        return toResponse(fhirId, req);
    }

    // GET ONE
    public TemplateDocumentResponse getOne(String fhirId) {
        log.debug("Getting template document: {}", fhirId);
        DocumentReference doc = fhirClientService.read(DocumentReference.class, fhirId, getPracticeId());
        if (doc == null) {
            throw new ResponseStatusException(NOT_FOUND, "Template not found");
        }
        return fromFhirDocumentReference(doc);
    }

    // GET ALL with optional filters
    public List<TemplateDocumentResponse> getAll(TemplateContext context, String q) {
        log.debug("Getting all template documents, context={}, q={}", context, q);
        Bundle bundle = fhirClientService.search(DocumentReference.class, getPracticeId());

        return fhirClientService.extractResources(bundle, DocumentReference.class).stream()
                .filter(this::isHtmlTemplate)
                .map(this::fromFhirDocumentReference)
                .filter(r -> context == null || context.equals(r.context))
                .filter(r -> q == null || q.isBlank() || (r.name != null && r.name.toLowerCase().contains(q.toLowerCase())))
                .collect(Collectors.toList());
    }

    // DELETE
    public void delete(String fhirId) {
        log.debug("Deleting template document: {}", fhirId);
        fhirClientService.delete(DocumentReference.class, fhirId, getPracticeId());
        log.info("Deleted template document id={}", fhirId);
    }

    // GET HTML RAW
    public String getHtmlRaw(String fhirId) {
        log.debug("Getting raw HTML for template: {}", fhirId);
        DocumentReference doc = fhirClientService.read(DocumentReference.class, fhirId, getPracticeId());
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

        String fhirId = doc.getIdElement().getIdPart();
        r.id = (long) Math.abs(fhirId.hashCode());

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
        r.id = (long) Math.abs(fhirId.hashCode());
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
