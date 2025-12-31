package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import com.qiaben.ciyex.dto.MonthlyOrderCountDto;
import com.qiaben.ciyex.dto.OrderDto;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-only Order Service.
 * Uses FHIR SupplyRequest resource directly via FhirClientService.
 * No local database storage - all data stored in FHIR server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    // Extension URLs
    private static final String EXT_ORDER_NUMBER = "http://ciyex.com/fhir/StructureDefinition/order-number";
    private static final String EXT_SUPPLIER = "http://ciyex.com/fhir/StructureDefinition/supplier";
    private static final String EXT_DATE = "http://ciyex.com/fhir/StructureDefinition/order-date";
    private static final String EXT_STOCK = "http://ciyex.com/fhir/StructureDefinition/stock";
    private static final String EXT_ITEM_NAME = "http://ciyex.com/fhir/StructureDefinition/item-name";
    private static final String EXT_CATEGORY = "http://ciyex.com/fhir/StructureDefinition/category";
    private static final String EXT_AMOUNT = "http://ciyex.com/fhir/StructureDefinition/amount";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public OrderDto create(OrderDto dto) {
        log.debug("Creating FHIR SupplyRequest (order): {}", dto.getOrderNumber());

        // Generate order number if missing
        if (dto.getOrderNumber() == null) {
            dto.setOrderNumber("PO-" + System.currentTimeMillis());
        }

        SupplyRequest sr = toFhirSupplyRequest(dto);
        var outcome = fhirClientService.create(sr, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        log.info("Created FHIR SupplyRequest (order) with id: {}", fhirId);

        return dto;
    }

    // GET BY ID
    public OrderDto getById(String fhirId) {
        log.debug("Getting FHIR SupplyRequest (order): {}", fhirId);
        SupplyRequest sr = fhirClientService.read(SupplyRequest.class, fhirId, getPracticeId());
        return fromFhirSupplyRequest(sr);
    }

    // GET ALL
    public List<OrderDto> getAll() {
        log.debug("Getting all FHIR SupplyRequests (orders)");

        Bundle bundle = fhirClientService.search(SupplyRequest.class, getPracticeId());
        List<SupplyRequest> requests = fhirClientService.extractResources(bundle, SupplyRequest.class);

        return requests.stream()
                .map(this::fromFhirSupplyRequest)
                .collect(Collectors.toList());
    }

    // GET ALL (Paginated)
    public Page<OrderDto> getAll(Pageable pageable) {
        List<OrderDto> all = getAll();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<OrderDto> pageContent = all.subList(start, end);
        return new PageImpl<>(pageContent, pageable, all.size());
    }

    // UPDATE
    public OrderDto update(String fhirId, OrderDto dto) {
        log.debug("Updating FHIR SupplyRequest (order): {}", fhirId);

        SupplyRequest sr = toFhirSupplyRequest(dto);
        sr.setId(fhirId);
        fhirClientService.update(sr, getPracticeId());

        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        return dto;
    }

    // DELETE
    public void delete(String fhirId) {
        log.debug("Deleting FHIR SupplyRequest (order): {}", fhirId);
        fhirClientService.delete(SupplyRequest.class, fhirId, getPracticeId());
    }

    // RECEIVE ORDER
    public OrderDto receiveOrder(String orderId, OrderDto dto) {
        log.debug("Receiving FHIR SupplyRequest (order): {}", orderId);
        
        SupplyRequest sr = fhirClientService.read(SupplyRequest.class, orderId, getPracticeId());
        OrderDto existing = fromFhirSupplyRequest(sr);
        
        // Update status to received
        existing.setStatus("Received");
        existing.setDate(LocalDateTime.now().toLocalDate().toString());
        
        if (dto != null) {
            if (dto.getStock() != null) existing.setStock(dto.getStock());
            if (dto.getAmount() != null) existing.setAmount(dto.getAmount());
            if (dto.getSupplier() != null) existing.setSupplier(dto.getSupplier());
        }
        
        return update(orderId, existing);
    }

    // CREATE ORDER (from inventory)
    public OrderDto createOrder(String inventoryName, String inventoryCategory, Integer stock, String supplier) {
        OrderDto dto = new OrderDto();
        dto.setOrderNumber("PO-" + System.currentTimeMillis());
        dto.setSupplier(supplier);
        dto.setDate(LocalDateTime.now().toLocalDate().toString());
        dto.setStatus("Pending");
        dto.setStock(stock);
        dto.setItemName(inventoryName);
        dto.setCategory(inventoryCategory);
        dto.setAmount(0.0);
        
        return create(dto);
    }

    // COUNT ORDERS BY MONTH (simplified - returns empty for FHIR)
    public List<MonthlyOrderCountDto> countOrdersByMonth() {
        // This would require aggregation which FHIR doesn't support natively
        // Return empty list - implement with custom logic if needed
        return new ArrayList<>();
    }

    // COUNT PENDING
    public long countPending() {
        return getAll().stream()
                .filter(o -> "Pending".equals(o.getStatus()))
                .count();
    }

    // -------- FHIR Mapping --------

    private SupplyRequest toFhirSupplyRequest(OrderDto dto) {
        SupplyRequest sr = new SupplyRequest();

        // Status
        if (dto.getStatus() != null) {
            switch (dto.getStatus().toLowerCase()) {
                case "pending" -> sr.setStatus(SupplyRequest.SupplyRequestStatus.ACTIVE);
                case "received" -> sr.setStatus(SupplyRequest.SupplyRequestStatus.COMPLETED);
                case "cancelled" -> sr.setStatus(SupplyRequest.SupplyRequestStatus.CANCELLED);
                default -> sr.setStatus(SupplyRequest.SupplyRequestStatus.ACTIVE);
            }
        }

        // Item (as CodeableConcept)
        if (dto.getItemName() != null) {
            CodeableConcept item = new CodeableConcept();
            item.setText(dto.getItemName());
            sr.setItem(item);
        }

        // Quantity
        if (dto.getStock() != null) {
            Quantity qty = new Quantity();
            qty.setValue(dto.getStock());
            sr.setQuantity(qty);
        }

        // Extensions
        addStringExtension(sr, EXT_ORDER_NUMBER, dto.getOrderNumber());
        addStringExtension(sr, EXT_SUPPLIER, dto.getSupplier());
        addStringExtension(sr, EXT_DATE, dto.getDate());
        addStringExtension(sr, EXT_ITEM_NAME, dto.getItemName());
        addStringExtension(sr, EXT_CATEGORY, dto.getCategory());
        
        if (dto.getStock() != null) {
            sr.addExtension(new Extension(EXT_STOCK, new IntegerType(dto.getStock())));
        }
        if (dto.getAmount() != null) {
            sr.addExtension(new Extension(EXT_AMOUNT, new DecimalType(dto.getAmount())));
        }

        return sr;
    }

    private OrderDto fromFhirSupplyRequest(SupplyRequest sr) {
        OrderDto dto = new OrderDto();
        dto.setFhirId(sr.getIdElement().getIdPart());
        dto.setExternalId(sr.getIdElement().getIdPart());

        // Status
        if (sr.hasStatus()) {
            switch (sr.getStatus()) {
                case ACTIVE -> dto.setStatus("Pending");
                case COMPLETED -> dto.setStatus("Received");
                case CANCELLED -> dto.setStatus("Cancelled");
                default -> dto.setStatus("Pending");
            }
        }

        // Item name from CodeableConcept
        if (sr.hasItemCodeableConcept()) {
            dto.setItemName(sr.getItemCodeableConcept().getText());
        }

        // Quantity
        if (sr.hasQuantity()) {
            dto.setStock(sr.getQuantity().getValue().intValue());
        }

        // Extensions
        dto.setOrderNumber(getExtensionString(sr, EXT_ORDER_NUMBER));
        dto.setSupplier(getExtensionString(sr, EXT_SUPPLIER));
        dto.setDate(getExtensionString(sr, EXT_DATE));
        dto.setCategory(getExtensionString(sr, EXT_CATEGORY));
        
        // Override item name from extension if present
        String itemNameExt = getExtensionString(sr, EXT_ITEM_NAME);
        if (itemNameExt != null) dto.setItemName(itemNameExt);

        Extension stockExt = sr.getExtensionByUrl(EXT_STOCK);
        if (stockExt != null && stockExt.getValue() instanceof IntegerType) {
            dto.setStock(((IntegerType) stockExt.getValue()).getValue());
        }

        Extension amountExt = sr.getExtensionByUrl(EXT_AMOUNT);
        if (amountExt != null && amountExt.getValue() instanceof DecimalType) {
            dto.setAmount(((DecimalType) amountExt.getValue()).getValue().doubleValue());
        }

        return dto;
    }

    // -------- Helpers --------

    private void addStringExtension(SupplyRequest sr, String url, String value) {
        if (value != null) {
            sr.addExtension(new Extension(url, new StringType(value)));
        }
    }

    private String getExtensionString(SupplyRequest sr, String url) {
        Extension ext = sr.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) {
            return ((StringType) ext.getValue()).getValue();
        }
        return null;
    }
}
