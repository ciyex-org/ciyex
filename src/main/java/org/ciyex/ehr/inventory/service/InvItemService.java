package org.ciyex.ehr.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.inventory.dto.*;
import org.ciyex.ehr.inventory.entity.*;
import org.ciyex.ehr.inventory.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvItemService {

    private final InventoryItemRepository itemRepo;
    private final InventoryCategoryRepository categoryRepo;
    private final InventoryLocationRepository locationRepo;
    private final InvSupplierRepository supplierRepo;
    private final InventoryAdjustmentRepository adjustmentRepo;

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    // ── CRUD ──

    @Transactional
    public InvItemDto create(InvItemDto dto) {
        var item = InventoryItem.builder()
                .name(dto.getName())
                .sku(dto.getSku())
                .description(dto.getDescription())
                .unit(dto.getUnit())
                .costPerUnit(dto.getCostPerUnit() != null ? dto.getCostPerUnit() : BigDecimal.ZERO)
                .stockOnHand(dto.getStockOnHand() != null ? dto.getStockOnHand() : 0)
                .minStock(dto.getMinStock() != null ? dto.getMinStock() : 0)
                .maxStock(dto.getMaxStock())
                .reorderPoint(dto.getReorderPoint() != null ? dto.getReorderPoint() : 0)
                .reorderQty(dto.getReorderQty())
                .status(dto.getStatus() != null ? dto.getStatus() : "active")
                .itemType(dto.getItemType() != null ? dto.getItemType() : "consumable")
                .barcode(dto.getBarcode())
                .manufacturer(dto.getManufacturer())
                .costMethod(dto.getCostMethod() != null ? dto.getCostMethod() : "fifo")
                .orgAlias(orgAlias())
                .build();

        if (dto.getCategoryId() != null) {
            categoryRepo.findById(dto.getCategoryId()).ifPresent(item::setCategory);
        }
        if (dto.getLocationId() != null) {
            locationRepo.findById(dto.getLocationId()).ifPresent(item::setLocation);
        }
        if (dto.getSupplierId() != null) {
            supplierRepo.findById(dto.getSupplierId()).ifPresent(item::setSupplier);
        }

        return toDto(itemRepo.save(item));
    }

    @Transactional(readOnly = true)
    public InvItemDto getById(Long id) {
        return itemRepo.findById(id)
                .filter(i -> i.getOrgAlias().equals(orgAlias()))
                .map(this::toDto)
                .orElseThrow(() -> new NoSuchElementException("Item not found: " + id));
    }

    @Transactional
    public InvItemDto update(Long id, InvItemDto dto) {
        var item = itemRepo.findById(id)
                .filter(i -> i.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Item not found: " + id));

        item.setName(dto.getName());
        item.setSku(dto.getSku());
        item.setDescription(dto.getDescription());
        item.setUnit(dto.getUnit());
        if (dto.getCostPerUnit() != null) item.setCostPerUnit(dto.getCostPerUnit());
        if (dto.getStockOnHand() != null) item.setStockOnHand(dto.getStockOnHand());
        if (dto.getMinStock() != null) item.setMinStock(dto.getMinStock());
        item.setMaxStock(dto.getMaxStock());
        if (dto.getReorderPoint() != null) item.setReorderPoint(dto.getReorderPoint());
        item.setReorderQty(dto.getReorderQty());
        if (dto.getStatus() != null) item.setStatus(dto.getStatus());
        if (dto.getItemType() != null) item.setItemType(dto.getItemType());
        item.setBarcode(dto.getBarcode());
        item.setManufacturer(dto.getManufacturer());
        if (dto.getCostMethod() != null) item.setCostMethod(dto.getCostMethod());

        if (dto.getCategoryId() != null) {
            categoryRepo.findById(dto.getCategoryId()).ifPresent(item::setCategory);
        } else {
            item.setCategory(null);
        }
        if (dto.getLocationId() != null) {
            locationRepo.findById(dto.getLocationId()).ifPresent(item::setLocation);
        } else {
            item.setLocation(null);
        }
        if (dto.getSupplierId() != null) {
            supplierRepo.findById(dto.getSupplierId()).ifPresent(item::setSupplier);
        } else {
            item.setSupplier(null);
        }

        return toDto(itemRepo.save(item));
    }

    @Transactional
    public void delete(Long id) {
        var item = itemRepo.findById(id)
                .filter(i -> i.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Item not found: " + id));
        itemRepo.delete(item);
    }

    // ── List / Search ──

    @Transactional(readOnly = true)
    public Page<InvItemDto> getAll(Pageable pageable) {
        return itemRepo.findByOrgAlias(orgAlias(), pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<InvItemDto> getAll() {
        return itemRepo.findByOrgAlias(orgAlias()).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Page<InvItemDto> search(String query, Pageable pageable) {
        return itemRepo.searchByName(orgAlias(), query, pageable).map(this::toDto);
    }

    // ── Analytics / Dashboard ──

    @Transactional(readOnly = true)
    public long countAll() {
        return itemRepo.countByOrgAlias(orgAlias());
    }

    @Transactional(readOnly = true)
    public Map<String, Long> countLowAndCritical() {
        String org = orgAlias();
        List<InventoryItem> lowItems = itemRepo.findLowStockItems(org);
        long low = lowItems.stream().filter(i -> i.getStockOnHand() > 0).count();
        long critical = lowItems.stream().filter(i -> i.getStockOnHand() == 0).count();
        return Map.of("low", low, "critical", critical);
    }

    @Transactional(readOnly = true)
    public List<InvItemDto> getLowStockItems() {
        return itemRepo.findLowStockItems(orgAlias()).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<InvItemDto> getExpiringItems(int days) {
        return itemRepo.findExpiringItems(orgAlias(), LocalDate.now().plusDays(days))
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalInventoryValue() {
        return itemRepo.findByOrgAlias(orgAlias()).stream()
                .map(i -> i.getCostPerUnit().multiply(BigDecimal.valueOf(i.getStockOnHand())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCategoryBreakdown() {
        return itemRepo.findByOrgAlias(orgAlias()).stream()
                .collect(Collectors.groupingBy(
                        i -> i.getCategory() != null ? i.getCategory().getName() : "Uncategorized",
                        Collectors.counting()))
                .entrySet().stream()
                .map(e -> Map.<String, Object>of("category", e.getKey(), "count", e.getValue()))
                .toList();
    }

    // ── Stock Adjustment ──

    @Transactional
    public InvAdjustmentDto adjustStock(InvAdjustmentDto dto) {
        var item = itemRepo.findById(dto.getItemId())
                .filter(i -> i.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Item not found: " + dto.getItemId()));

        int newStock = item.getStockOnHand() + dto.getQuantityChange();
        if (newStock < 0) {
            throw new IllegalArgumentException("Stock cannot go below zero. Current: " + item.getStockOnHand());
        }
        item.setStockOnHand(newStock);
        itemRepo.save(item);

        var adj = InventoryAdjustment.builder()
                .item(item)
                .quantityChange(dto.getQuantityChange())
                .reasonCode(dto.getReasonCode())
                .notes(dto.getNotes())
                .adjustedBy(dto.getAdjustedBy())
                .referenceType(dto.getReferenceType())
                .referenceId(dto.getReferenceId())
                .orgAlias(orgAlias())
                .build();
        adj = adjustmentRepo.save(adj);

        return InvAdjustmentDto.builder()
                .id(adj.getId())
                .itemId(item.getId())
                .itemName(item.getName())
                .quantityChange(adj.getQuantityChange())
                .reasonCode(adj.getReasonCode())
                .notes(adj.getNotes())
                .adjustedBy(adj.getAdjustedBy())
                .referenceType(adj.getReferenceType())
                .referenceId(adj.getReferenceId())
                .createdAt(adj.getCreatedAt() != null ? adj.getCreatedAt().toString() : null)
                .build();
    }

    @Transactional(readOnly = true)
    public List<InvAdjustmentDto> getAdjustments(Long itemId) {
        return adjustmentRepo.findByItemIdOrderByCreatedAtDesc(itemId).stream()
                .map(a -> InvAdjustmentDto.builder()
                        .id(a.getId())
                        .itemId(a.getItem().getId())
                        .itemName(a.getItem().getName())
                        .quantityChange(a.getQuantityChange())
                        .reasonCode(a.getReasonCode())
                        .notes(a.getNotes())
                        .adjustedBy(a.getAdjustedBy())
                        .referenceType(a.getReferenceType())
                        .referenceId(a.getReferenceId())
                        .createdAt(a.getCreatedAt() != null ? a.getCreatedAt().toString() : null)
                        .build())
                .toList();
    }

    // ── Mapping ──

    private InvItemDto toDto(InventoryItem e) {
        return InvItemDto.builder()
                .id(e.getId())
                .name(e.getName())
                .sku(e.getSku())
                .description(e.getDescription())
                .unit(e.getUnit())
                .costPerUnit(e.getCostPerUnit())
                .stockOnHand(e.getStockOnHand())
                .minStock(e.getMinStock())
                .maxStock(e.getMaxStock())
                .reorderPoint(e.getReorderPoint())
                .reorderQty(e.getReorderQty())
                .status(e.getStatus())
                .itemType(e.getItemType())
                .barcode(e.getBarcode())
                .manufacturer(e.getManufacturer())
                .costMethod(e.getCostMethod())
                .categoryId(e.getCategory() != null ? e.getCategory().getId() : null)
                .categoryName(e.getCategory() != null ? e.getCategory().getName() : null)
                .locationId(e.getLocation() != null ? e.getLocation().getId() : null)
                .locationName(e.getLocation() != null ? e.getLocation().getName() : null)
                .supplierId(e.getSupplier() != null ? e.getSupplier().getId() : null)
                .supplierName(e.getSupplier() != null ? e.getSupplier().getName() : null)
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }
}
