package org.ciyex.ehr.controller;

import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.inventory.dto.*;
import org.ciyex.ehr.inventory.service.*;
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
import java.util.Map;

@PreAuthorize("hasAuthority('SCOPE_user/Organization.read')")
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InvItemService itemService;
    private final InvStockService stockService;
    private final InvDashboardService dashboardService;

    // ── Item CRUD ──

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<ApiResponse<InvItemDto>> create(@Valid @RequestBody InvItemDto dto) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Inventory item created successfully", itemService.create(dto)));
        } catch (Exception e) {
            log.error("Failed to create inventory item", e);
            return ResponseEntity.ok(ApiResponse.error("Failed to create inventory item: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvItemDto>> get(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Inventory item retrieved", itemService.getById(id)));
        } catch (Exception e) {
            log.error("Failed to get inventory item {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Item not found: " + id));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<ApiResponse<InvItemDto>> update(@PathVariable Long id, @Valid @RequestBody InvItemDto dto) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Inventory item updated", itemService.update(id, dto)));
        } catch (Exception e) {
            log.error("Failed to update inventory item {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed to update: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            itemService.delete(id);
            return ResponseEntity.ok(ApiResponse.ok("Inventory item deleted", null));
        } catch (Exception e) {
            log.error("Failed to delete inventory item {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed to delete: " + e.getMessage()));
        }
    }

    // ── List / Search ──

    @GetMapping
    public ResponseEntity<ApiResponse<Page<InvItemDto>>> getAll(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<InvItemDto> page = (search != null && !search.isBlank())
                ? itemService.search(search, pageable)
                : itemService.getAll(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Items retrieved", page));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<InvItemDto>>> listAll() {
        return ResponseEntity.ok(ApiResponse.ok("Items retrieved", itemService.getAll()));
    }

    // ── Analytics / Dashboard ──

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getCount() {
        return ResponseEntity.ok(ApiResponse.ok("Count retrieved", itemService.countAll()));
    }

    @GetMapping("/low-critical")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getLowAndCritical() {
        return ResponseEntity.ok(ApiResponse.ok("Low/critical counts retrieved", itemService.countLowAndCritical()));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<InvItemDto>>> getLowStock() {
        return ResponseEntity.ok(ApiResponse.ok("Low stock items retrieved", itemService.getLowStockItems()));
    }

    @GetMapping("/expiring")
    public ResponseEntity<ApiResponse<List<InvItemDto>>> getExpiring(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(ApiResponse.ok("Expiring items retrieved", itemService.getExpiringItems(days)));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<InvDashboardDto>> getDashboard() {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Dashboard retrieved", dashboardService.getDashboard()));
        } catch (Exception e) {
            log.error("Failed to get dashboard", e);
            return ResponseEntity.ok(ApiResponse.error("Failed to get dashboard: " + e.getMessage()));
        }
    }

    // ── Stock Adjustments ──

    @PostMapping("/{id}/adjust")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<ApiResponse<InvAdjustmentDto>> adjustStock(
            @PathVariable Long id, @Valid @RequestBody InvAdjustmentDto dto) {
        try {
            dto.setItemId(id);
            return ResponseEntity.ok(ApiResponse.ok("Stock adjusted", itemService.adjustStock(dto)));
        } catch (Exception e) {
            log.error("Failed to adjust stock for item {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed to adjust: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/adjustments")
    public ResponseEntity<ApiResponse<List<InvAdjustmentDto>>> getAdjustments(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Adjustments retrieved", itemService.getAdjustments(id)));
    }

    // ── Lots ──

    @PostMapping("/{id}/lots")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<ApiResponse<InvLotDto>> createLot(
            @PathVariable Long id, @Valid @RequestBody InvLotDto dto) {
        try {
            dto.setItemId(id);
            return ResponseEntity.ok(ApiResponse.ok("Lot created", stockService.createLot(dto)));
        } catch (Exception e) {
            log.error("Failed to create lot for item {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed to create lot: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/lots")
    public ResponseEntity<ApiResponse<List<InvLotDto>>> getLots(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Lots retrieved", stockService.getLotsForItem(id)));
    }

    // ── Waste ──

    @PostMapping("/{id}/waste")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<ApiResponse<InvWasteDto>> logWaste(
            @PathVariable Long id, @Valid @RequestBody InvWasteDto dto) {
        try {
            dto.setItemId(id);
            return ResponseEntity.ok(ApiResponse.ok("Waste logged", stockService.logWaste(dto)));
        } catch (Exception e) {
            log.error("Failed to log waste for item {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed to log waste: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/waste")
    public ResponseEntity<ApiResponse<List<InvWasteDto>>> getWaste(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Waste log retrieved", stockService.getWasteLog(id)));
    }

    // ── Categories ──

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<InvCategoryDto>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.ok("Categories retrieved", stockService.getCategories()));
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<ApiResponse<InvCategoryDto>> createCategory(@RequestBody InvCategoryDto dto) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Category created", stockService.createCategory(dto)));
        } catch (Exception e) {
            log.error("Failed to create category", e);
            return ResponseEntity.ok(ApiResponse.error("Failed to create category: " + e.getMessage()));
        }
    }

    // ── Locations ──

    @GetMapping("/locations")
    public ResponseEntity<ApiResponse<List<InvLocationDto>>> getLocations() {
        return ResponseEntity.ok(ApiResponse.ok("Locations retrieved", stockService.getLocations()));
    }

    @PostMapping("/locations")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<ApiResponse<InvLocationDto>> createLocation(@RequestBody InvLocationDto dto) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Location created", stockService.createLocation(dto)));
        } catch (Exception e) {
            log.error("Failed to create location", e);
            return ResponseEntity.ok(ApiResponse.error("Failed to create location: " + e.getMessage()));
        }
    }
}
