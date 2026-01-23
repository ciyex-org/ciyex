package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.OrderDto;
import com.qiaben.ciyex.service.OrderService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@Slf4j
public class OrdersController {

    private final OrderService service;

    public OrdersController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderDto>> create(@Valid @RequestBody OrderDto dto, BindingResult result) {
        try {
            if (result.hasErrors()) {
                StringBuilder errorMsg = new StringBuilder();
                result.getFieldErrors().forEach(error -> 
                    errorMsg.append(error.getDefaultMessage()).append(", ")
                );
                if (errorMsg.length() > 0) {
                    errorMsg.setLength(errorMsg.length() - 2);
                }
                return ResponseEntity.ok(ApiResponse.<OrderDto>builder()
                        .success(false)
                        .message(errorMsg.toString())
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
    public ResponseEntity<ApiResponse<OrderDto>> get(@PathVariable("id") String id) {
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
    public ResponseEntity<ApiResponse<OrderDto>> update(@PathVariable("id") String id, @Valid @RequestBody OrderDto dto, BindingResult result) {
        try {
            if (result.hasErrors()) {
                StringBuilder errorMsg = new StringBuilder();
                result.getFieldErrors().forEach(error -> 
                    errorMsg.append(error.getDefaultMessage()).append(", ")
                );
                if (errorMsg.length() > 0) {
                    errorMsg.setLength(errorMsg.length() - 2);
                }
                return ResponseEntity.ok(ApiResponse.<OrderDto>builder()
                        .success(false)
                        .message(errorMsg.toString())
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
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") String id) {
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
            @PathVariable("orderId") String orderId,
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
}
