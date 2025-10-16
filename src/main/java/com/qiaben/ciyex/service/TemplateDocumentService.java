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
    private final TenantSchemaInitializer tenantSchemaInitializer;

    public TemplateDocumentService(TemplateDocumentRepository repository,
                                   TenantSchemaInitializer tenantSchemaInitializer) {
        this.repository = repository;
        this.tenantSchemaInitializer = tenantSchemaInitializer;
    }

    // --- CRUD ---

    @Transactional
    public TemplateDocumentResponse create(TemplateDocumentUpsertRequest req) {
        Long currentOrgId = getCurrentOrgIdOrThrow("create");
        // If body provided orgId, enforce it matches current org
        if (req.orgId != null && !req.orgId.equals(currentOrgId)) {
            log.error("Org mismatch during create: body.orgId={} != ctx.orgId={}", req.orgId, currentOrgId);
            throw new SecurityException("Body orgId does not match request context orgId");
        }

        // Ensure orgId is set for entity
        Long finalOrgId = (req.orgId != null) ? req.orgId : currentOrgId;

        // Initialize tenant schema explicitly (like PatientService)
        tenantSchemaInitializer.initializeTenantSchema(finalOrgId);

        var e = new TemplateDocumentEntity();
        e.setOrgId(finalOrgId);
        e.setContext(req.context);
        e.setName(req.name);
        e.setContent(req.content);
        e.setOptions(req.options);

        e = repository.save(e);

        log.info("Created template document id={} orgId={} name='{}'", e.getId(), finalOrgId, e.getName());
        return toResponse(e);
    }

    @Transactional
    public TemplateDocumentResponse update(Long id, TemplateDocumentUpsertRequest req) {
        Long currentOrgId = getCurrentOrgIdOrThrow("update");

        var e = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Template not found"));

        // Enforce tenant scoping
        if (!currentOrgId.equals(e.getOrgId())) {
            log.error("Access denied on update: template id={} belongs to orgId={}, ctx.orgId={}", id, e.getOrgId(), currentOrgId);
            throw new SecurityException("Access denied: Template does not belong to current org");
        }

        // If body has orgId ensure it matches the entity/current org
        if (req.orgId != null && !req.orgId.equals(currentOrgId)) {
            log.error("Org mismatch during update: body.orgId={} != ctx.orgId={}", req.orgId, currentOrgId);
            throw new SecurityException("Body orgId does not match request context orgId");
        }

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
        Long currentOrgId = getCurrentOrgIdOrThrow("getOne");

        var e = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Template not found"));

        if (!currentOrgId.equals(e.getOrgId())) {
            log.error("Access denied on getOne: template id={} belongs to orgId={}, ctx.orgId={}", id, e.getOrgId(), currentOrgId);
            throw new SecurityException("Access denied: Template does not belong to current org");
        }

        return toResponse(e);
    }

    /**
     * org-aware listing with optional filters
     * Matches controller signature: getAll(orgId, context, q)
     */
    @Transactional(readOnly = true)
    public List<TemplateDocumentResponse> getAll(Long orgId, TemplateContext context, String q) {
        Long currentOrgId = getCurrentOrgIdOrThrow("list");

        // Enforce that the requested org matches the current org scope
        Long effectiveOrgId = (orgId != null) ? orgId : currentOrgId;
        if (!effectiveOrgId.equals(currentOrgId)) {
            log.error("Requested orgId {} does not match ctx orgId {}", effectiveOrgId, currentOrgId);
            throw new ResponseStatusException(BAD_REQUEST, "Requested orgId does not match request context");
        }



        List<TemplateDocumentEntity> data;
        boolean hasQ = (q != null && !q.isBlank());

        if (context != null && hasQ) {
            data = repository.findByOrgIdAndContextAndNameContainingIgnoreCase(effectiveOrgId, context, q.trim());
        } else if (context != null) {
            data = repository.findByOrgIdAndContext(effectiveOrgId, context);
        } else if (hasQ) {
            data = repository.findByOrgIdAndNameContainingIgnoreCase(effectiveOrgId, q.trim());
        } else {
            data = repository.findByOrgId(effectiveOrgId);
        }

        return data.stream().map(this::toResponse).toList();
    }

    @Transactional
    public void delete(Long id) {
        Long currentOrgId = getCurrentOrgIdOrThrow("delete");

        var e = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Template not found"));

        if (!currentOrgId.equals(e.getOrgId())) {
            log.error("Access denied on delete: template id={} belongs to orgId={}, ctx.orgId={}", id, e.getOrgId(), currentOrgId);
            throw new SecurityException("Access denied: Template does not belong to current org");
        }

        repository.delete(e);
        log.info("Deleted template document id={} orgId={}", id, currentOrgId);
    }

    /** RAW html exactly as stored (for /{id}/html) */
    @Transactional(readOnly = true)
    public String getHtmlRaw(Long id) {
        Long currentOrgId = getCurrentOrgIdOrThrow("getHtmlRaw");

        var e = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Template not found"));

        if (!currentOrgId.equals(e.getOrgId())) {
            log.error("Access denied on getHtmlRaw: template id={} belongs to orgId={}, ctx.orgId={}", id, e.getOrgId(), currentOrgId);
            throw new SecurityException("Access denied: Template does not belong to current org");
        }

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

    private Long getCurrentOrgIdOrThrow(String action) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        if (orgId == null) {
            log.error("No orgId found in RequestContext during {}", action);
            throw new SecurityException("No orgId available in request context");
        }
        return orgId;
    }
}
