package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.TemplateDocumentResponse;
import com.qiaben.ciyex.dto.TemplateDocumentUpsertRequest;
import com.qiaben.ciyex.entity.TemplateContext;
import com.qiaben.ciyex.service.TemplateDocumentService;
import com.qiaben.ciyex.dto.integration.RequestContext;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/template-documents")
public class TemplateDocumentController {

    private final TemplateDocumentService service;

    public TemplateDocumentController(TemplateDocumentService service) {
        this.service = service;
    }

    // Create (orgId in body)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public TemplateDocumentResponse create(@Valid @RequestBody TemplateDocumentUpsertRequest body) {
        // If caller provided orgId in body (and no X-Org-Id header was sent), ensure RequestContext has it
        try {
            RequestContext ctx = RequestContext.get();
            if ((ctx == null || ctx.getOrgId() == null) && body.orgId != null) {
                if (ctx == null) ctx = new RequestContext();
                ctx.setOrgId(body.orgId);
                RequestContext.set(ctx);
            }
        } catch (Exception ignored) {
            // best-effort; proceed and let Tenant interceptors handle missing orgId
        }

        return service.create(body);
    }

    // Get one (by id). orgId can be provided by X-Org-Id header (preferred) or ?orgId= query param
    @GetMapping(value = "/{id:\\d+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public TemplateDocumentResponse getOne(@PathVariable Long id, @RequestParam(value = "orgId", required = false) Long orgIdParam) {
        RequestContext ctx = RequestContext.get();
        Long orgId = (ctx != null ? ctx.getOrgId() : null);
        if (orgId == null) orgId = orgIdParam;
        if (orgId == null) throw new ResponseStatusException(BAD_REQUEST, "X-Org-Id header or orgId query parameter required to scope request");

        // ensure RequestContext contains orgId for downstream processing
        try {
            if (ctx == null) {
                ctx = new RequestContext();
                ctx.setOrgId(orgId);
                RequestContext.set(ctx);
            } else if (ctx.getOrgId() == null) {
                ctx.setOrgId(orgId);
                RequestContext.set(ctx);
            }
        } catch (Exception ignored) {}

        return service.getOne(id);
    }

    // Get all for org (orgId optional if provided as header). Optional filters: context & q
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TemplateDocumentResponse> getAll(
            @RequestParam(value = "orgId", required = false) Long orgIdParam,
            @RequestParam(value = "context", required = false) TemplateContext context,
            @RequestParam(value = "q", required = false) String q) {
        RequestContext ctx = RequestContext.get();
        Long orgId = (ctx != null ? ctx.getOrgId() : null);
        if (orgId == null) orgId = orgIdParam;
        if (orgId == null) throw new ResponseStatusException(BAD_REQUEST, "X-Org-Id header or orgId query parameter required to scope request");

        // ensure RequestContext contains orgId
        try {
            if (ctx == null) {
                ctx = new RequestContext();
                ctx.setOrgId(orgId);
                RequestContext.set(ctx);
            } else if (ctx.getOrgId() == null) {
                ctx.setOrgId(orgId);
                RequestContext.set(ctx);
            }
        } catch (Exception ignored) {}

        return service.getAll(orgId, context, q);
    }

    // Filter by context via path (e.g. /filter/ENCOUNTER or /filter/PORTAL)
    @GetMapping(value = "/filter/{context}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TemplateDocumentResponse> filterByContext(@PathVariable String context,
                                                          @RequestParam(value = "orgId", required = false) Long orgIdParam) {
        TemplateContext ctxEnum = null;
        try {
            ctxEnum = TemplateContext.valueOf(context.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid context: " + context);
        }

        RequestContext reqCtx = RequestContext.get();
        Long orgId = (reqCtx != null ? reqCtx.getOrgId() : null);
        if (orgId == null) orgId = orgIdParam;
        if (orgId == null) throw new ResponseStatusException(BAD_REQUEST, "X-Org-Id header or orgId query parameter required to scope request");

        try {
            if (reqCtx == null) {
                reqCtx = new RequestContext();
                reqCtx.setOrgId(orgId);
                RequestContext.set(reqCtx);
            } else if (reqCtx.getOrgId() == null) {
                reqCtx.setOrgId(orgId);
                RequestContext.set(reqCtx);
            }
        } catch (Exception ignored) {}

        return service.getAll(orgId, ctxEnum, null);
    }

    // Update (orgId in body)
    @PutMapping(value = "/{id:\\d+}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public TemplateDocumentResponse update(@PathVariable Long id, @Valid @RequestBody TemplateDocumentUpsertRequest body) {
        // ensure RequestContext contains orgId from body if header not provided
        try {
            RequestContext ctx = RequestContext.get();
            if ((ctx == null || ctx.getOrgId() == null) && body.orgId != null) {
                if (ctx == null) ctx = new RequestContext();
                ctx.setOrgId(body.orgId);
                RequestContext.set(ctx);
            }
        } catch (Exception ignored) {
        }

        return service.update(id, body);
    }

    // Delete
    @DeleteMapping("/{id:\\d+}")
    public void delete(@PathVariable Long id) {
        RequestContext ctx = RequestContext.get();
        if (ctx == null || ctx.getOrgId() == null) throw new ResponseStatusException(BAD_REQUEST, "X-Org-Id header or orgId required to scope request");
        service.delete(id);
    }

    // Live HTML preview
    @GetMapping(value = "/{id:\\d+}/html", produces = MediaType.ALL_VALUE)
    public ResponseEntity<String> getHtml(@PathVariable Long id) {
        RequestContext ctx = RequestContext.get();
        if (ctx == null || ctx.getOrgId() == null) throw new ResponseStatusException(BAD_REQUEST, "X-Org-Id header or orgId required to scope request");
        String html = service.getHtmlRaw(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"template.html\"")
                .body(html);
    }
}
