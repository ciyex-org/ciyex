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
        return service.create(body);
    }

    // Get one (by id). orgId can be provided by X-Org-Id header (preferred) or ?orgId= query param
    @GetMapping(value = "/{id:\\d+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public TemplateDocumentResponse getOne(@PathVariable Long id) {
        return service.getOne(id);
    }

    // Get all for org (orgId optional if provided as header). Optional filters: context & q
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TemplateDocumentResponse> getAll(
            @RequestParam(value = "context", required = false) TemplateContext context,
            @RequestParam(value = "q", required = false) String q) {
        return service.getAll(context, q);
    }

    // Filter by context via path (e.g. /filter/ENCOUNTER or /filter/PORTAL)
    @GetMapping(value = "/filter/{context}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TemplateDocumentResponse> filterByContext(@PathVariable String context) {
        TemplateContext ctxEnum = null;
        try {
            ctxEnum = TemplateContext.valueOf(context.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid context: " + context);
        }
        return service.getAll(ctxEnum, null);
    }

    // Update (orgId in body)
    @PutMapping(value = "/{id:\\d+}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public TemplateDocumentResponse update(@PathVariable Long id, @Valid @RequestBody TemplateDocumentUpsertRequest body) {
        return service.update(id, body);
    }

    // Delete
    @DeleteMapping("/{id:\\d+}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    // Live HTML preview
    @GetMapping(value = "/{id:\\d+}/html", produces = MediaType.ALL_VALUE)
    public ResponseEntity<String> getHtml(@PathVariable Long id) {
        String html = service.getHtmlRaw(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"template.html\"")
                .body(html);
    }
}

