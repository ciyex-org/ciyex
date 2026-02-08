package org.ciyex.ehr.controller;

import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.InventoryDto;
import org.ciyex.ehr.dto.OrderDto;
import org.ciyex.ehr.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InventoryDto>> create(@Valid @RequestBody InventoryDto dto) {
        try {
            InventoryDto created = service.create(dto);
            return ResponseEntity.ok(
                    ApiResponse.<InventoryDto>builder()
                            .success(true)
                            .message("Inventory item created successfully")
                            .data(created)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to create inventory item", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.<InventoryDto>builder()
                            .success(false)
                            .message("Failed to create inventory item: " + e.getMessage())
                            .build()
            );
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryDto>> get(@PathVariable String id) {
        try {
            InventoryDto item = service.getById(id);
            return ResponseEntity.ok(
                    ApiResponse.<InventoryDto>builder()
                            .success(true)
                            .message("Inventory item retrieved successfully")
                            .data(item)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to get inventory item with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.<InventoryDto>builder()
                            .success(false)
                            .message("Inventory item not found with id: " + id)
                            .build()
            );
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryDto>> update(@PathVariable String id, @Valid @RequestBody InventoryDto dto) {
        try {
            InventoryDto updated = service.update(id, dto);
            return ResponseEntity.ok(
                    ApiResponse.<InventoryDto>builder()
                            .success(true)
                            .message("Inventory item updated successfully")
                            .data(updated)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to update inventory item with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.<InventoryDto>builder()
                            .success(false)
                            .message("Inventory item not found with id: " + id)
                            .build()
            );
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(
                    ApiResponse.<Void>builder()
                            .success(true)
                            .message("Inventory item deleted successfully")
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to delete inventory item with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.<Void>builder()
                            .success(false)
                            .message("Inventory item not found with id: " + id)
                            .build()
            );
        }
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

    @PostMapping("/{id}/reorder")
    public ResponseEntity<ApiResponse<OrderDto>> reorder(
            @PathVariable String id,
            @RequestBody OrderDto dto) {
        try {
            OrderDto createdOrder = service.createReorder(id, dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.<OrderDto>builder()
                            .success(true)
                            .message("Reorder created successfully (pending receipt)")
                            .data(createdOrder)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.<OrderDto>builder()
                            .success(false)
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            log.error("Reorder failed for inventory id {}: {}", id, e.getMessage(), e);
            String msg = e.getMessage() != null && e.getMessage().contains("not found") 
                ? "Inventory item not found with id: " + id
                : "Failed to create reorder: " + e.getMessage();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.<OrderDto>builder()
                            .success(false)
                            .message(msg)
                            .data(null)
                            .build()
            );
        } catch (Throwable t) {
            log.error("Reorder failed for inventory id {}: {}", id, t.getMessage(), t);
            String msg = t instanceof StackOverflowError
                ? "Internal server error: recursion detected while processing reorder (StackOverflowError). Please check server logs and contact support."
                : "An unexpected error occurred while processing reorder. Please check server logs and contact support.";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<OrderDto>builder()
                            .success(false)
                            .message(msg)
                            .data(null)
                            .build()
            );
        }
    }

    @GetMapping("/records/weekly-consumption")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getWeeklyConsumption() {
        List<Map<String, Object>> data = service.getWeeklyConsumption();
        return ResponseEntity.ok(
                ApiResponse.<List<Map<String, Object>>>builder()
                        .success(true)
                        .message("Weekly stock consumption retrieved successfully")
                        .data(data)
                        .build()
        );
    }

    @GetMapping("/records/monthly-orders")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMonthlyOrders() {
        List<Map<String, Object>> data = service.getMonthlyOrders();
        return ResponseEntity.ok(
                ApiResponse.<List<Map<String, Object>>>builder()
                        .success(true)
                        .message("Monthly orders retrieved successfully")
                        .data(data)
                        .build()
        );
    }

    // InventoryController.java
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getCount() {
        long count = service.countAll();
        return ResponseEntity.ok(ApiResponse.<Long>builder()
                .success(true)
                .message("Total SKUs count retrieved successfully")
                .data(count)
                .build());
    }

    @GetMapping("/low-critical")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getLowAndCritical() {
        Map<String, Long> counts = service.countLowAndCritical();
        return ResponseEntity.ok(ApiResponse.<Map<String, Long>>builder()
                .success(true)
                .message("Low and critical counts retrieved successfully")
                .data(counts)
                .build());
    }

}
