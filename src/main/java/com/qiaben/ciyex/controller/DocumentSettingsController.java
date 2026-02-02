package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.DocumentSettingsDto;
import com.qiaben.ciyex.service.DocumentSettingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/document-settings")
@Slf4j
public class DocumentSettingsController {

    private final DocumentSettingsService service;

    public DocumentSettingsController(DocumentSettingsService service) {
        this.service = service;
    }

    // --- Settings ---

    @GetMapping()
    public ResponseEntity<ApiResponse<DocumentSettingsDto>> get() {
        try {
            var data = service.get();
            String message = "Settings retrieved successfully";
            return ResponseEntity.ok(ApiResponse.<DocumentSettingsDto>builder()
                    .success(true).message(message).data(data).build());
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.<DocumentSettingsDto>builder()
                    .success(false).message(e.getMessage()).build());
        }
    }

    @PutMapping
    public ResponseEntity<ApiResponse<DocumentSettingsDto>> save(@RequestBody DocumentSettingsDto dto) {
        try {
            var data = service.save(dto, "admin"); // replace with logged-in user
            return ResponseEntity.ok(ApiResponse.<DocumentSettingsDto>builder()
                    .success(true).message("Settings saved").data(data).build());
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.<DocumentSettingsDto>builder()
                    .success(false).message(e.getMessage()).build());
        }
    }

    // --- Categories ---

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<DocumentSettingsDto.Category>>> getCategories() {
        try {
            var data = service.getCategories();
            return ResponseEntity.ok(ApiResponse.<List<DocumentSettingsDto.Category>>builder()
                    .success(true).data(data).build());
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.<List<DocumentSettingsDto.Category>>builder()
                    .success(false).message(e.getMessage()).build());
        }
    }

    @PostMapping("/categories/{name}/{active}")
    public ResponseEntity<ApiResponse<List<DocumentSettingsDto.Category>>> addCategory(
            @PathVariable String name,
            @PathVariable boolean active) {
        try {
            var data = service.addCategory(name, active, "admin");
            return ResponseEntity.ok(ApiResponse.<List<DocumentSettingsDto.Category>>builder()
                    .success(true).message("Category added: " + name).data(data).build());
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.<List<DocumentSettingsDto.Category>>builder()
                    .success(false).message(e.getMessage()).build());
        }
    }

    @DeleteMapping("/categories/{name}")
    public ResponseEntity<ApiResponse<List<DocumentSettingsDto.Category>>> deleteCategory(
            @PathVariable String name) {
        try {
            var data = service.deleteCategory(name, "admin");
            return ResponseEntity.ok(ApiResponse.<List<DocumentSettingsDto.Category>>builder()
                    .success(true).message("Category deleted: " + name).data(data).build());
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.<List<DocumentSettingsDto.Category>>builder()
                    .success(false).message(e.getMessage()).build());
        }
    }

    @DeleteMapping("/categories")
    public ResponseEntity<ApiResponse<List<DocumentSettingsDto.Category>>> deleteAllCategories() {
        try {
            var data = service.deleteAllCategories("admin");
            return ResponseEntity.ok(ApiResponse.<List<DocumentSettingsDto.Category>>builder()
                    .success(true).message("All categories deleted").data(data).build());
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.<List<DocumentSettingsDto.Category>>builder()
                    .success(false).message(e.getMessage()).build());
        }
    }
}