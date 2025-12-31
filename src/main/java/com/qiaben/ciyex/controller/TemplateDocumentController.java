    package com.qiaben.ciyex.controller;

    import com.qiaben.ciyex.dto.TemplateDocumentResponse;
    import com.qiaben.ciyex.dto.TemplateDocumentUpsertRequest;
    import com.qiaben.ciyex.entity.TemplateContext;
    import com.qiaben.ciyex.service.TemplateDocumentService;
    import com.qiaben.ciyex.exception.ResourceNotFoundException;
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

        // Create
        @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
        public TemplateDocumentResponse create(@Valid @RequestBody TemplateDocumentUpsertRequest body) {
            return service.create(body);
        }

        // Get one (by id)
        @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
        public TemplateDocumentResponse getOne(@PathVariable String id) {
            return service.getOne(id);
        }

        // Get all. Optional filters: context & q
        @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
        public List<TemplateDocumentResponse> getAll(
                @RequestParam(value = "context", required = false) TemplateContext context,
                @RequestParam(value = "q", required = false) String q) {
            return service.getAll(context, q);
        }

        // Filter by context via path (e.g. /filter/ENCOUNTER or /filter/PORTAL)
        @GetMapping(value = "/filter/{context}", produces = MediaType.APPLICATION_JSON_VALUE)
        public List<TemplateDocumentResponse> filterByContext(@PathVariable String context) {
            try {
                TemplateContext ctxEnum = TemplateContext.valueOf(context.toUpperCase());
                return service.getAll(ctxEnum, null);
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(BAD_REQUEST, "Invalid context: " + context);
            }
        }

        // Update
        @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
        public TemplateDocumentResponse update(@PathVariable String id, @Valid @RequestBody TemplateDocumentUpsertRequest body) {
            return service.update(id, body);
        }

        // Delete
        @DeleteMapping("/{id}")
        public void delete(@PathVariable String id) {
            service.delete(id);
        }

        // Live HTML preview
        @GetMapping(value = "/{id}/html", produces = MediaType.ALL_VALUE)
        public ResponseEntity<String> getHtml(@PathVariable String id) {
            String html = service.getHtmlRaw(id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"template.html\"")
                    .body(html);
        }
    }

