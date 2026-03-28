package org.ciyex.ehr.education.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.education.dto.EducationMaterialDto;
import org.ciyex.ehr.education.service.EducationMaterialService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.NoSuchElementException;

@PreAuthorize("hasAuthority('SCOPE_user/Communication.read')")
@RestController
@RequestMapping("/api/education/materials")
@RequiredArgsConstructor
@Slf4j
public class EducationMaterialController {

    private final EducationMaterialService service;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category) {
        try {
            var pageable = PageRequest.of(page, size);
            if (q != null && !q.isBlank()) {
                List<EducationMaterialDto> list = service.search(q);
                Page<EducationMaterialDto> pageResult = new PageImpl<>(list, pageable, list.size());
                return ResponseEntity.ok(ApiResponse.ok("Materials retrieved", pageResult));
            }
            if (category != null && !category.isBlank()) {
                List<EducationMaterialDto> list = service.getByCategory(category);
                Page<EducationMaterialDto> pageResult = new PageImpl<>(list, pageable, list.size());
                return ResponseEntity.ok(ApiResponse.ok("Materials retrieved", pageResult));
            }
            var results = service.getAll(pageable);
            return ResponseEntity.ok(ApiResponse.ok("Materials retrieved", results));
        } catch (Exception e) {
            log.error("Failed to list education materials", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EducationMaterialDto>> getById(@PathVariable Long id) {
        try {
            var material = service.getById(id);
            return ResponseEntity.ok(ApiResponse.ok("Material retrieved", material));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get education material {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_user/Communication.write')")
    public ResponseEntity<ApiResponse<EducationMaterialDto>> create(@RequestBody EducationMaterialDto dto) {
        try {
            var created = service.create(dto);
            return ResponseEntity.ok(ApiResponse.ok("Material created", created));
        } catch (Exception e) {
            log.error("Failed to create education material", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Communication.write')")
    public ResponseEntity<ApiResponse<EducationMaterialDto>> update(
            @PathVariable Long id, @RequestBody EducationMaterialDto dto) {
        try {
            var updated = service.update(id, dto);
            return ResponseEntity.ok(ApiResponse.ok("Material updated", updated));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update education material {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Communication.write')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.ok("Material deleted", null));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete education material {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/view")
    @PreAuthorize("hasAuthority('SCOPE_user/Communication.write')")
    public ResponseEntity<ApiResponse<Void>> incrementViewCount(@PathVariable Long id) {
        try {
            service.incrementViewCount(id);
            return ResponseEntity.ok(ApiResponse.ok("View count incremented", null));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to increment view count for material {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }
}
