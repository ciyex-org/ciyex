package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.AdminTemplateDto;
import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.service.AdminTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

import java.util.List;

@RestController
@RequestMapping("/api/admin/templates")
@RequiredArgsConstructor
@Slf4j
public class AdminTemplateController {

    private final AdminTemplateService service;

    @PostMapping
    public ResponseEntity<ApiResponse<AdminTemplateDto>> create(@RequestBody AdminTemplateDto dto) {
        try {
            AdminTemplateDto created = service.create(dto);
            return ResponseEntity.ok(ApiResponse.<AdminTemplateDto>builder()
                    .success(true)
                    .message("Admin template created successfully")
                    .data(created)
                    .build());
        } catch (Exception e) {
            log.error("Error creating AdminTemplate", e);
            return ResponseEntity.internalServerError().body(ApiResponse.<AdminTemplateDto>builder()
                    .success(false)
                    .message("Failed to create admin template")
                    .build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminTemplateDto>> getById(@PathVariable Long id) {
        try {
            AdminTemplateDto dto = service.getById(id);
            return ResponseEntity.ok(ApiResponse.<AdminTemplateDto>builder()
                    .success(true)
                    .message("Admin template retrieved successfully")
                    .data(dto)
                    .build());
        } catch (Exception e) {
            log.error("Error retrieving AdminTemplate with id {}", id, e);
            return ResponseEntity.internalServerError().body(ApiResponse.<AdminTemplateDto>builder()
                    .success(false)
                    .message("Failed to retrieve admin template")
                    .build());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminTemplateDto>> update(@PathVariable Long id,
                                                                @RequestBody AdminTemplateDto dto) {
        try {
            AdminTemplateDto updated = service.update(id, dto);
            return ResponseEntity.ok(ApiResponse.<AdminTemplateDto>builder()
                    .success(true)
                    .message("Admin template updated successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Error updating AdminTemplate with id {}", id, e);
            return ResponseEntity.internalServerError().body(ApiResponse.<AdminTemplateDto>builder()
                    .success(false)
                    .message("Failed to update admin template")
                    .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Admin template deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Error deleting AdminTemplate with id {}", id, e);
            return ResponseEntity.internalServerError().body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete admin template")
                    .build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminTemplateDto>>> getAll(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        try {
            // If pagination params provided, return paginated results and include total count header
            if (page != null && size != null) {
                Page<AdminTemplateDto> p = service.getPaginated(page, size);
                ApiResponse<List<AdminTemplateDto>> body = ApiResponse.<List<AdminTemplateDto>>builder()
                        .success(true)
                        .message("Admin templates retrieved successfully")
                        .data(p.getContent())
                        .build();
                return ResponseEntity.ok()
                        .header("X-Total-Count", String.valueOf(p.getTotalElements()))
                        .body(body);
            }

            ApiResponse<List<AdminTemplateDto>> response = service.getAll();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving admin templates", e);
            return ResponseEntity.internalServerError().body(ApiResponse.<List<AdminTemplateDto>>builder()
                    .success(false)
                    .message("Failed to retrieve admin templates")
                    .build());
        }
    }
}
