package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.TemplateDto;
import com.qiaben.ciyex.service.TemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@Slf4j
public class TemplateController {

    private final TemplateService service;

    public TemplateController(TemplateService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TemplateDto>> create(@RequestBody TemplateDto dto) {
        try {
            TemplateDto created = service.create(dto);
            return ResponseEntity.ok(ApiResponse.<TemplateDto>builder()
                    .success(true)
                    .message("Template created successfully")
                    .data(created)
                    .build());
        } catch (Exception e) {
            log.error("Failed to create template: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<TemplateDto>builder()
                    .success(false)
                    .message("Failed to create template: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TemplateDto>> get(@PathVariable Long id) {
        try {
            TemplateDto dto = service.getById(id);
            return ResponseEntity.ok(ApiResponse.<TemplateDto>builder()
                    .success(true)
                    .message("Template retrieved successfully")
                    .data(dto)
                    .build());
        } catch (Exception e) {
            log.error("Failed to get template: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<TemplateDto>builder()
                    .success(false)
                    .message("Failed to get template: " + e.getMessage())
                    .build());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TemplateDto>> update(@PathVariable Long id, @RequestBody TemplateDto dto) {
        try {
            TemplateDto updated = service.update(id, dto);
            return ResponseEntity.ok(ApiResponse.<TemplateDto>builder()
                    .success(true)
                    .message("Template updated successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update template: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<TemplateDto>builder()
                    .success(false)
                    .message("Failed to update template: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Template deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete template: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete template: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TemplateDto>>> getAll() {
        try {
            ApiResponse<List<TemplateDto>> response = service.getAllTemplates();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to retrieve templates: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<List<TemplateDto>>builder()
                    .success(false)
                    .message("Failed to retrieve templates: " + e.getMessage())
                    .build());
        }
    }
}
