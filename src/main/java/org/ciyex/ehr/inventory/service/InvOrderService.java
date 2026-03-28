package org.ciyex.ehr.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.inventory.dto.InvOrderDto;
import org.ciyex.ehr.inventory.dto.InvOrderLineDto;
import org.ciyex.ehr.inventory.entity.*;
import org.ciyex.ehr.inventory.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvOrderService {

    private final PurchaseOrderRepository orderRepo;
    private final PurchaseOrderLineRepository lineRepo;
    private final InvSupplierRepository supplierRepo;
    private final InventoryItemRepository itemRepo;
    private final InventoryAdjustmentRepository adjustmentRepo;
    private final InventoryLotRepository lotRepo;

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    // ── CRUD ──

    @Transactional
    public InvOrderDto create(InvOrderDto dto) {
        var po = PurchaseOrder.builder()
                .poNumber(dto.getPoNumber() != null ? dto.getPoNumber() : generatePoNumber())
                .status(dto.getStatus() != null ? dto.getStatus() : "draft")
                .orderDate(dto.getOrderDate() != null && !dto.getOrderDate().isBlank() ? LocalDate.parse(dto.getOrderDate()) : LocalDate.now())
                .expectedDate(dto.getExpectedDate() != null && !dto.getExpectedDate().isBlank() ? LocalDate.parse(dto.getExpectedDate()) : null)
                .notes(dto.getNotes())
                .createdBy(dto.getCreatedBy())
                .approvedBy(dto.getApprovedBy())
                .orgAlias(orgAlias())
                .build();

        if (dto.getSupplierId() != null) {
            supplierRepo.findById(dto.getSupplierId()).ifPresent(po::setSupplier);
        }

        // Save PO first to get the ID
        po = orderRepo.save(po);

        // Add line items
        if (dto.getLines() != null) {
            BigDecimal total = BigDecimal.ZERO;
            for (var lineDto : dto.getLines()) {
                var line = PurchaseOrderLine.builder()
                        .order(po)
                        .itemName(lineDto.getItemName())
                        .quantityOrdered(lineDto.getQuantityOrdered() != null ? lineDto.getQuantityOrdered() : 0)
                        .quantityReceived(0)
                        .unitCost(lineDto.getUnitCost() != null ? lineDto.getUnitCost() : BigDecimal.ZERO)
                        .lotNumber(lineDto.getLotNumber())
                        .expiryDate(lineDto.getExpiryDate() != null && !lineDto.getExpiryDate().isBlank() ? LocalDate.parse(lineDto.getExpiryDate()) : null)
                        .notes(lineDto.getNotes())
                        .build();

                if (lineDto.getItemId() != null) {
                    itemRepo.findById(lineDto.getItemId()).ifPresent(item -> {
                        line.setItem(item);
                        if (line.getItemName() == null) line.setItemName(item.getName());
                    });
                }
                po.getLines().add(line);

                if (lineDto.getQuantityOrdered() != null && lineDto.getUnitCost() != null) {
                    total = total.add(lineDto.getUnitCost().multiply(BigDecimal.valueOf(lineDto.getQuantityOrdered())));
                }
            }
            po.setTotalAmount(total);
        }

        po = orderRepo.save(po);
        return toDto(po);
    }

    @Transactional(readOnly = true)
    public InvOrderDto getById(Long id) {
        return orderRepo.findById(id)
                .filter(o -> o.getOrgAlias().equals(orgAlias()))
                .map(this::toDto)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + id));
    }

    @Transactional
    public InvOrderDto update(Long id, InvOrderDto dto) {
        var po = orderRepo.findById(id)
                .filter(o -> o.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + id));

        if (dto.getSupplierId() != null) {
            supplierRepo.findById(dto.getSupplierId()).ifPresent(po::setSupplier);
        }
        if (dto.getStatus() != null) po.setStatus(dto.getStatus());
        if (dto.getExpectedDate() != null && !dto.getExpectedDate().isBlank()) po.setExpectedDate(LocalDate.parse(dto.getExpectedDate()));
        po.setNotes(dto.getNotes());
        po.setApprovedBy(dto.getApprovedBy());

        // Update line items
        if (dto.getLines() != null) {
            po.getLines().clear();
            BigDecimal total = BigDecimal.ZERO;
            for (var lineDto : dto.getLines()) {
                var line = PurchaseOrderLine.builder()
                        .order(po)
                        .itemName(lineDto.getItemName())
                        .quantityOrdered(lineDto.getQuantityOrdered() != null ? lineDto.getQuantityOrdered() : 0)
                        .quantityReceived(lineDto.getQuantityReceived() != null ? lineDto.getQuantityReceived() : 0)
                        .unitCost(lineDto.getUnitCost() != null ? lineDto.getUnitCost() : BigDecimal.ZERO)
                        .lotNumber(lineDto.getLotNumber())
                        .expiryDate(lineDto.getExpiryDate() != null && !lineDto.getExpiryDate().isBlank() ? LocalDate.parse(lineDto.getExpiryDate()) : null)
                        .notes(lineDto.getNotes())
                        .build();

                if (lineDto.getItemId() != null) {
                    itemRepo.findById(lineDto.getItemId()).ifPresent(item -> {
                        line.setItem(item);
                        if (line.getItemName() == null) line.setItemName(item.getName());
                    });
                }
                po.getLines().add(line);

                if (lineDto.getQuantityOrdered() != null && lineDto.getUnitCost() != null) {
                    total = total.add(lineDto.getUnitCost().multiply(BigDecimal.valueOf(lineDto.getQuantityOrdered())));
                }
            }
            po.setTotalAmount(total);
        }

        return toDto(orderRepo.save(po));
    }

    @Transactional
    public void delete(Long id) {
        var po = orderRepo.findById(id)
                .filter(o -> o.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + id));
        orderRepo.delete(po);
    }

    // ── List ──

    @Transactional(readOnly = true)
    public Page<InvOrderDto> getAll(Pageable pageable) {
        return orderRepo.findByOrgAlias(orgAlias(), pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<InvOrderDto> getAll() {
        return orderRepo.findByOrgAliasAndStatus(orgAlias(), null).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return orderRepo.countByOrgAliasAndStatus(orgAlias(), "submitted")
                + orderRepo.countByOrgAliasAndStatus(orgAlias(), "draft");
    }

    // ── Receive ──

    @Transactional
    public InvOrderDto receiveOrder(Long orderId, Map<Long, Integer> lineReceiveQty) {
        var po = orderRepo.findById(orderId)
                .filter(o -> o.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));

        boolean allReceived = true;
        for (var line : po.getLines()) {
            Integer receivedQty = lineReceiveQty != null ? lineReceiveQty.get(line.getId()) : null;
            if (receivedQty == null) receivedQty = line.getQuantityOrdered(); // full receive by default

            line.setQuantityReceived(line.getQuantityReceived() + receivedQty);

            // Update item stock
            if (line.getItem() != null) {
                var item = line.getItem();
                item.setStockOnHand(item.getStockOnHand() + receivedQty);
                itemRepo.save(item);

                // Create adjustment record
                adjustmentRepo.save(InventoryAdjustment.builder()
                        .item(item)
                        .quantityChange(receivedQty)
                        .reasonCode("received")
                        .notes("Received from PO " + po.getPoNumber())
                        .referenceType("purchase_order")
                        .referenceId(po.getId())
                        .orgAlias(orgAlias())
                        .build());

                // Create lot if lot number present
                if (line.getLotNumber() != null && !line.getLotNumber().isBlank()) {
                    lotRepo.save(InventoryLot.builder()
                            .item(item)
                            .lotNumber(line.getLotNumber())
                            .expiryDate(line.getExpiryDate())
                            .quantity(receivedQty)
                            .costPerUnit(line.getUnitCost())
                            .orgAlias(orgAlias())
                            .build());
                }
            }

            if (line.getQuantityReceived() < line.getQuantityOrdered()) {
                allReceived = false;
            }
        }

        po.setStatus(allReceived ? "received" : "partial");
        po.setReceivedDate(LocalDate.now());
        return toDto(orderRepo.save(po));
    }

    // ── Helpers ──

    private String generatePoNumber() {
        long count = orderRepo.countByOrgAlias(orgAlias());
        return "PO-" + String.format("%05d", count + 1);
    }

    private InvOrderDto toDto(PurchaseOrder e) {
        return InvOrderDto.builder()
                .id(e.getId())
                .poNumber(e.getPoNumber())
                .supplierId(e.getSupplier() != null ? e.getSupplier().getId() : null)
                .supplierName(e.getSupplier() != null ? e.getSupplier().getName() : null)
                .status(e.getStatus())
                .orderDate(e.getOrderDate() != null ? e.getOrderDate().toString() : null)
                .expectedDate(e.getExpectedDate() != null ? e.getExpectedDate().toString() : null)
                .receivedDate(e.getReceivedDate() != null ? e.getReceivedDate().toString() : null)
                .notes(e.getNotes())
                .totalAmount(e.getTotalAmount())
                .createdBy(e.getCreatedBy())
                .approvedBy(e.getApprovedBy())
                .lines(e.getLines() != null ? e.getLines().stream().map(this::toLineDto).toList() : List.of())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }

    private InvOrderLineDto toLineDto(PurchaseOrderLine l) {
        return InvOrderLineDto.builder()
                .id(l.getId())
                .itemId(l.getItem() != null ? l.getItem().getId() : null)
                .itemName(l.getItemName())
                .quantityOrdered(l.getQuantityOrdered())
                .quantityReceived(l.getQuantityReceived())
                .unitCost(l.getUnitCost())
                .totalCost(l.getTotalCost())
                .lotNumber(l.getLotNumber())
                .expiryDate(l.getExpiryDate() != null ? l.getExpiryDate().toString() : null)
                .notes(l.getNotes())
                .build();
    }
}
