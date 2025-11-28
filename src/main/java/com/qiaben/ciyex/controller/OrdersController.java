package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.OrderDto;
import com.qiaben.ciyex.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Slf4j
public class OrdersController {

    private final OrderService service;

    public OrdersController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderDto>> create(@RequestBody OrderDto dto) {
        try {
            // Validate mandatory fields
            String validationError = validateMandatoryFields(dto);
            if (validationError != null) {
                return ResponseEntity.ok(ApiResponse.<OrderDto>builder()
                        .success(false)
                        .message(validationError)
                        .build());
            }

            return ResponseEntity.ok(ApiResponse.<OrderDto>builder()
                    .success(true)
                    .message("Order created successfully")
                    .data(service.create(dto))
                    .build());
        } catch (Exception e) {
            log.error("Failed to create Order: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<OrderDto>builder()
                    .success(false)
                    .message("Failed to create order: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDto>> get(@PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.<OrderDto>builder()
                    .success(true)
                    .message("Order retrieved successfully")
                    .data(service.getById(id))
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve Order with id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<OrderDto>builder()
                    .success(false)
                    .message("Failed to retrieve order: " + e.getMessage())
                    .build());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDto>> update(@PathVariable("id") Long id, @RequestBody OrderDto dto) {
        try {
            // Validate mandatory fields
            String validationError = validateMandatoryFields(dto);
            if (validationError != null) {
                return ResponseEntity.ok(ApiResponse.<OrderDto>builder()
                        .success(false)
                        .message(validationError)
                        .build());
            }

            return ResponseEntity.ok(ApiResponse.<OrderDto>builder()
                    .success(true)
                    .message("Order updated successfully")
                    .data(service.update(id, dto))
                    .build());
        } catch (Exception e) {
            log.error("Failed to update Order with id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<OrderDto>builder()
                    .success(false)
                    .message("Failed to update order: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Order deleted successfully")
                    .data(null)
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete Order with id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete order: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderDto>>> getAll(@PageableDefault Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<Page<OrderDto>>builder()
                .success(true)
                .message("Orders retrieved successfully")
                .data(service.getAll(pageable))
                .build());
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<OrderDto>>> listAll() {
        return ResponseEntity.ok(ApiResponse.<List<OrderDto>>builder()
                .success(true)
                .message("Orders retrieved successfully")
                .data(service.getAll())
                .build());
    }

    @PutMapping("/{orderId}/receive")
    public ResponseEntity<ApiResponse<OrderDto>> receiveOrder(
            @PathVariable("orderId") Long orderId,
            @RequestBody(required = false) OrderDto dto
    ) {
        try {
            OrderDto order = service.receiveOrder(orderId, dto);
            return ResponseEntity.ok(ApiResponse.<OrderDto>builder()
                    .success(true)
                    .message("Order marked as received and inventory updated")
                    .data(order)
                    .build());
        } catch (Exception e) {
            log.error("Failed to receive Order with id {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<OrderDto>builder()
                    .success(false)
                    .message("Failed to receive order: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/pending/count")
    public ResponseEntity<ApiResponse<Long>> getPendingCount() {
        long count = service.countPending();
        return ResponseEntity.ok(ApiResponse.<Long>builder()
                .success(true)
                .message("Pending orders count retrieved successfully")
                .data(count)
                .build());
    }

    /**
     * Validates mandatory fields for Order creation and update
     * @param dto OrderDto to validate
     * @return error message if validation fails, null if validation passes
     */
    private String validateMandatoryFields(OrderDto dto) {
        StringBuilder missingFields = new StringBuilder();

        if (dto.getOrderNumber() == null || dto.getOrderNumber().trim().isEmpty()) {
            missingFields.append("orderNumber, ");
        }

        if (dto.getSupplier() == null || dto.getSupplier().trim().isEmpty()) {
            missingFields.append("supplier, ");
        }

        if (dto.getStatus() == null || dto.getStatus().trim().isEmpty()) {
            missingFields.append("status, ");
        }

        if (dto.getItemName() == null || dto.getItemName().trim().isEmpty()) {
            missingFields.append("itemName, ");
        }

        if (dto.getCategory() == null || dto.getCategory().trim().isEmpty()) {
            missingFields.append("category, ");
        }

        if (!missingFields.isEmpty()) {
            // Remove the trailing comma and space
            missingFields.setLength(missingFields.length() - 2);
            return "Missing mandatory fields: " + missingFields;
        }

        return null;
    }

}
