package org.ciyex.ehr.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.inventory.dto.*;
import org.ciyex.ehr.inventory.entity.*;
import org.ciyex.ehr.inventory.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvStockService {

    private final InventoryLotRepository lotRepo;
    private final WasteLogRepository wasteRepo;
    private final InventoryItemRepository itemRepo;
    private final InventoryAdjustmentRepository adjustmentRepo;
    private final InventoryCategoryRepository categoryRepo;
    private final InventoryLocationRepository locationRepo;

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    // ── Lots ──

    @Transactional
    public InvLotDto createLot(InvLotDto dto) {
        var item = itemRepo.findById(dto.getItemId())
                .filter(i -> i.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Item not found: " + dto.getItemId()));

        var lot = InventoryLot.builder()
                .item(item)
                .lotNumber(dto.getLotNumber())
                .expiryDate(dto.getExpiryDate() != null && !dto.getExpiryDate().isBlank() ? LocalDate.parse(dto.getExpiryDate()) : null)
                .quantity(dto.getQuantity())
                .receivedDate(dto.getReceivedDate() != null && !dto.getReceivedDate().isBlank() ? LocalDate.parse(dto.getReceivedDate()) : LocalDate.now())
                .costPerUnit(dto.getCostPerUnit())
                .notes(dto.getNotes())
                .orgAlias(orgAlias())
                .build();
        return toLotDto(lotRepo.save(lot));
    }

    @Transactional(readOnly = true)
    public List<InvLotDto> getLotsForItem(Long itemId) {
        return lotRepo.findByItemId(itemId).stream().map(this::toLotDto).toList();
    }

    @Transactional(readOnly = true)
    public List<InvLotDto> getExpiringLots(int days) {
        return lotRepo.findByOrgAliasAndExpiryDateBefore(orgAlias(), LocalDate.now().plusDays(days))
                .stream().map(this::toLotDto).toList();
    }

    // ── Waste Log ──

    @Transactional
    public InvWasteDto logWaste(InvWasteDto dto) {
        var item = itemRepo.findById(dto.getItemId())
                .filter(i -> i.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Item not found: " + dto.getItemId()));

        // Reduce stock
        int newStock = item.getStockOnHand() - dto.getQuantity();
        if (newStock < 0) {
            throw new IllegalArgumentException("Waste quantity exceeds stock on hand");
        }
        item.setStockOnHand(newStock);
        itemRepo.save(item);

        // Log adjustment
        adjustmentRepo.save(InventoryAdjustment.builder()
                .item(item)
                .quantityChange(-dto.getQuantity())
                .reasonCode(dto.getReasonCode())
                .notes("Waste: " + dto.getNotes())
                .adjustedBy(dto.getLoggedBy())
                .referenceType("waste")
                .orgAlias(orgAlias())
                .build());

        // Log waste
        var waste = WasteLog.builder()
                .item(item)
                .quantity(dto.getQuantity())
                .reasonCode(dto.getReasonCode())
                .notes(dto.getNotes())
                .loggedBy(dto.getLoggedBy())
                .orgAlias(orgAlias())
                .build();
        waste = wasteRepo.save(waste);

        return InvWasteDto.builder()
                .id(waste.getId())
                .itemId(item.getId())
                .itemName(item.getName())
                .quantity(waste.getQuantity())
                .reasonCode(waste.getReasonCode())
                .notes(waste.getNotes())
                .loggedBy(waste.getLoggedBy())
                .createdAt(waste.getCreatedAt() != null ? waste.getCreatedAt().toString() : null)
                .build();
    }

    @Transactional(readOnly = true)
    public List<InvWasteDto> getWasteLog(Long itemId) {
        return wasteRepo.findByItemIdOrderByCreatedAtDesc(itemId).stream()
                .map(w -> InvWasteDto.builder()
                        .id(w.getId())
                        .itemId(w.getItem().getId())
                        .itemName(w.getItem().getName())
                        .quantity(w.getQuantity())
                        .reasonCode(w.getReasonCode())
                        .notes(w.getNotes())
                        .loggedBy(w.getLoggedBy())
                        .createdAt(w.getCreatedAt() != null ? w.getCreatedAt().toString() : null)
                        .build())
                .toList();
    }

    // ── Categories ──

    @Transactional(readOnly = true)
    public List<InvCategoryDto> getCategories() {
        return categoryRepo.findByOrgAliasOrOrgAlias(orgAlias(), "__DEFAULT__").stream()
                .map(c -> InvCategoryDto.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .parentId(c.getParent() != null ? c.getParent().getId() : null)
                        .parentName(c.getParent() != null ? c.getParent().getName() : null)
                        .build())
                .toList();
    }

    @Transactional
    public InvCategoryDto createCategory(InvCategoryDto dto) {
        var cat = InventoryCategory.builder()
                .name(dto.getName())
                .orgAlias(orgAlias())
                .build();
        if (dto.getParentId() != null) {
            categoryRepo.findById(dto.getParentId()).ifPresent(cat::setParent);
        }
        cat = categoryRepo.save(cat);
        return InvCategoryDto.builder()
                .id(cat.getId())
                .name(cat.getName())
                .parentId(cat.getParent() != null ? cat.getParent().getId() : null)
                .parentName(cat.getParent() != null ? cat.getParent().getName() : null)
                .build();
    }

    // ── Locations ──

    @Transactional(readOnly = true)
    public List<InvLocationDto> getLocations() {
        return locationRepo.findByOrgAliasOrOrgAlias(orgAlias(), "__DEFAULT__").stream()
                .map(l -> InvLocationDto.builder()
                        .id(l.getId())
                        .name(l.getName())
                        .type(l.getType())
                        .parentId(l.getParent() != null ? l.getParent().getId() : null)
                        .parentName(l.getParent() != null ? l.getParent().getName() : null)
                        .build())
                .toList();
    }

    @Transactional
    public InvLocationDto createLocation(InvLocationDto dto) {
        var loc = InventoryLocation.builder()
                .name(dto.getName())
                .type(dto.getType() != null ? dto.getType() : "room")
                .orgAlias(orgAlias())
                .build();
        if (dto.getParentId() != null) {
            locationRepo.findById(dto.getParentId()).ifPresent(loc::setParent);
        }
        loc = locationRepo.save(loc);
        return InvLocationDto.builder()
                .id(loc.getId())
                .name(loc.getName())
                .type(loc.getType())
                .parentId(loc.getParent() != null ? loc.getParent().getId() : null)
                .parentName(loc.getParent() != null ? loc.getParent().getName() : null)
                .build();
    }

    // ── Mapping ──

    private InvLotDto toLotDto(InventoryLot e) {
        return InvLotDto.builder()
                .id(e.getId())
                .itemId(e.getItem().getId())
                .itemName(e.getItem().getName())
                .lotNumber(e.getLotNumber())
                .expiryDate(e.getExpiryDate() != null ? e.getExpiryDate().toString() : null)
                .quantity(e.getQuantity())
                .receivedDate(e.getReceivedDate() != null ? e.getReceivedDate().toString() : null)
                .costPerUnit(e.getCostPerUnit())
                .notes(e.getNotes())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .build();
    }
}
