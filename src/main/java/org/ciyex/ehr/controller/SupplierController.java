package org.ciyex.ehr.controller;

import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.inventory.dto.InvSupplierDto;
import org.ciyex.ehr.inventory.service.InvSupplierService2;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@PreAuthorize("hasAuthority('SCOPE_user/Organization.read')")
@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@Slf4j
public class SupplierController {

    private final InvSupplierService2 supplierService;

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<ApiResponse<InvSupplierDto>> create(@Valid @RequestBody InvSupplierDto dto) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Supplier created", supplierService.create(dto)));
        } catch (Exception e) {
            log.error("Failed to create supplier", e);
            return ResponseEntity.ok(ApiResponse.error("Failed to create supplier: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvSupplierDto>> get(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Supplier retrieved", supplierService.getById(id)));
        } catch (Exception e) {
            log.error("Failed to get supplier {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Supplier not found: " + id));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<ApiResponse<InvSupplierDto>> update(@PathVariable Long id, @Valid @RequestBody InvSupplierDto dto) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Supplier updated", supplierService.update(id, dto)));
        } catch (Exception e) {
            log.error("Failed to update supplier {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed to update: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            supplierService.delete(id);
            return ResponseEntity.ok(ApiResponse.ok("Supplier deleted", null));
        } catch (Exception e) {
            log.error("Failed to delete supplier {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed to delete: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<InvSupplierDto>>> getAll(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Suppliers retrieved", supplierService.getAll(pageable)));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<InvSupplierDto>>> listAll() {
        return ResponseEntity.ok(ApiResponse.ok("Suppliers retrieved", supplierService.getAll()));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getCount() {
        return ResponseEntity.ok(ApiResponse.ok("Supplier count retrieved", supplierService.count()));
    }
}
