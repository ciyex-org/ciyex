package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.TemplateDocumentResponse;
import com.qiaben.ciyex.dto.TemplateDocumentUpsertRequest;
import com.qiaben.ciyex.entity.TemplateContext;
import com.qiaben.ciyex.entity.TemplateDocumentEntity;
import com.qiaben.ciyex.repository.TemplateDocumentRepository;
import com.qiaben.ciyex.dto.integration.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Slf4j
public class TemplateDocumentService {

    private final TemplateDocumentRepository repository;

    public TemplateDocumentService(TemplateDocumentRepository repository){
        this.repository = repository;
    }

    // --- CRUD ---

    @Transactional
    public TemplateDocumentResponse create(TemplateDocumentUpsertRequest req) {

        // Initialize tenant schema explicitly (like PatientService)

        var e = new TemplateDocumentEntity();
        e.setContext(req.context);
        e.setName(req.name);
        e.setContent(req.content);
        e.setOptions(req.options);

        e = repository.save(e);

        log.info("Created template document id={} name='{}'", e.getId(), e.getName());
        return toResponse(e);
    }

    @Transactional
    public TemplateDocumentResponse update(Long id, TemplateDocumentUpsertRequest req) {

        var e = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Template not found"));

        e.setContext(req.context);
        e.setName(req.name);
        e.setContent(req.content);
        e.setOptions(req.options);

        e = repository.save(e);
        log.info("Updated template document id={} orgId={} name='{}'", e.getId(), e.getOrgId(), e.getName());

        return toResponse(e);
    }

    @Transactional(readOnly = true)
    public TemplateDocumentResponse getOne(Long id) {
        var e = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Template not found"));

        return toResponse(e);
    }

    /**
     * org-aware listing with optional filters
     * Matches controller signature: getAll(orgId, context, q)
     */
    @Transactional(readOnly = true)
    public List<TemplateDocumentResponse> getAll(Long orgId, TemplateContext context, String q) {
        List<TemplateDocumentEntity> data = List.of();
        boolean hasQ = (q != null && !q.isBlank());

       /* if (context != null && hasQ) {
            data = repository.findByContextAndNameContainingIgnoreCase(effectiveOrgId, context, q.trim());
        } else if (context != null) {
            data = repository.findByContext(effectiveOrgId, context);
        } else if (hasQ) {
            data = repository.findByNameContainingIgnoreCase(effectiveOrgId, q.trim());
        } else {
            data = repository.findByOrgId(effectiveOrgId);
        }*/

        return data.stream().map(this::toResponse).toList();
    }

    @Transactional
    public void delete(Long id) {

        var e = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Template not found"));



        repository.delete(e);
        log.info("Deleted template document id={}", id);
    }

    /** RAW html exactly as stored (for /{id}/html) */
    @Transactional(readOnly = true)
    public String getHtmlRaw(Long id) {


        var e = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Template not found"));

        return e.getContent();
    }

    // --- Helpers ---

    private TemplateDocumentResponse toResponse(TemplateDocumentEntity e) {
        var r = new TemplateDocumentResponse();
        r.id = e.getId();
        r.orgId = e.getOrgId();
        r.context = e.getContext();
        r.name = e.getName();
        r.content = e.getContent();
        r.options = e.getOptions();
        r.createdAt = e.getCreatedAt();
        r.updatedAt = e.getUpdatedAt();
        return r;
    }
}


