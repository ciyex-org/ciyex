package org.ciyex.ehr.controller;

import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.inventory.dto.InvOrderDto;
import org.ciyex.ehr.inventory.service.InvOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;

@PreAuthorize("hasAuthority('SCOPE_user/ServiceRequest.read')")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrdersController {

    private final InvOrderService orderService;

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_user/ServiceRequest.write')")
    public ResponseEntity<ApiResponse<InvOrderDto>> create(@Valid @RequestBody InvOrderDto dto) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Order created successfully", orderService.create(dto)));
        } catch (Exception e) {
            log.error("Failed to create order", e);
            return ResponseEntity.ok(ApiResponse.error("Failed to create order: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvOrderDto>> get(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Order retrieved", orderService.getById(id)));
        } catch (Exception e) {
            log.error("Failed to get order {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Order not found: " + id));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/ServiceRequest.write')")
    public ResponseEntity<ApiResponse<InvOrderDto>> update(@PathVariable Long id, @Valid @RequestBody InvOrderDto dto) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Order updated", orderService.update(id, dto)));
        } catch (Exception e) {
            log.error("Failed to update order {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed to update: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/ServiceRequest.write')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            orderService.delete(id);
            return ResponseEntity.ok(ApiResponse.ok("Order deleted", null));
        } catch (Exception e) {
            log.error("Failed to delete order {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed to delete: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<InvOrderDto>>> getAll(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Orders retrieved", orderService.getAll(pageable)));
    }

    @PutMapping("/{id}/receive")
    @PreAuthorize("hasAuthority('SCOPE_user/ServiceRequest.write')")
    public ResponseEntity<ApiResponse<InvOrderDto>> receiveOrder(
            @PathVariable Long id,
            @RequestBody(required = false) Map<Long, Integer> lineReceiveQty) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(
                    "Order received and inventory updated",
                    orderService.receiveOrder(id, lineReceiveQty)));
        } catch (Exception e) {
            log.error("Failed to receive order {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed to receive: " + e.getMessage()));
        }
    }

    @GetMapping("/pending/count")
    public ResponseEntity<ApiResponse<Long>> getPendingCount() {
        return ResponseEntity.ok(ApiResponse.ok("Pending count retrieved", orderService.countPending()));
    }
}
