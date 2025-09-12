package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.SupplierDto;
import com.qiaben.ciyex.service.SupplierService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService service;

    public SupplierController(SupplierService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SupplierDto>> create(@RequestBody SupplierDto dto) {
        return ResponseEntity.ok(ApiResponse.<SupplierDto>builder()
                .success(true)
                .message("Supplier created successfully")
                .data(service.create(dto))
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<SupplierDto>builder()
                .success(true)
                .message("Supplier retrieved successfully")
                .data(service.getById(id))
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierDto>> update(@PathVariable Long id, @RequestBody SupplierDto dto) {
        return ResponseEntity.ok(ApiResponse.<SupplierDto>builder()
                .success(true)
                .message("Supplier updated successfully")
                .data(service.update(id, dto))
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Supplier deleted successfully")
                .data(null)
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<SupplierDto>>> getAll(@PageableDefault Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<Page<SupplierDto>>builder()
                .success(true)
                .message("Suppliers retrieved successfully")
                .data(service.getAll(pageable))
                .build());
    }
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getCount() {
        return ResponseEntity.ok(ApiResponse.<Long>builder()
                .success(true)
                .message("Supplier count retrieved successfully")
                .data(service.countByOrg())
                .build());
    }

}
