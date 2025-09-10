package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.InventoryDto;
import com.qiaben.ciyex.service.InventoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InventoryDto>> create(@RequestBody InventoryDto dto) {
        InventoryDto created = service.create(dto);
        return ResponseEntity.ok(
                ApiResponse.<InventoryDto>builder()
                        .success(true)
                        .message("Inventory item created successfully")
                        .data(created)
                        .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryDto>> get(@PathVariable Long id) {
        InventoryDto item = service.getById(id);
        return ResponseEntity.ok(
                ApiResponse.<InventoryDto>builder()
                        .success(true)
                        .message("Inventory item retrieved successfully")
                        .data(item)
                        .build()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryDto>> update(@PathVariable Long id, @RequestBody InventoryDto dto) {
        InventoryDto updated = service.update(id, dto);
        return ResponseEntity.ok(
                ApiResponse.<InventoryDto>builder()
                        .success(true)
                        .message("Inventory item updated successfully")
                        .data(updated)
                        .build()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Inventory item deleted successfully")
                        .data(null)
                        .build()
        );
    }

    // Paginated list
    @GetMapping
    public ResponseEntity<ApiResponse<Page<InventoryDto>>> getAll(@PageableDefault Pageable pageable) {
        Page<InventoryDto> page = service.getAll(pageable);
        return ResponseEntity.ok(
                ApiResponse.<Page<InventoryDto>>builder()
                        .success(true)
                        .message("Inventory items retrieved successfully")
                        .data(page)
                        .build()
        );
    }

    // Non-paginated list (optional)
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<InventoryDto>>> listAll() {
        List<InventoryDto> list = service.getAll();
        return ResponseEntity.ok(
                ApiResponse.<List<InventoryDto>>builder()
                        .success(true)
                        .message("Inventory items retrieved successfully")
                        .data(list)
                        .build()
        );
    }
}
