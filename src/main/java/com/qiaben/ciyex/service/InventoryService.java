package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.InventoryDto;
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

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FHIR-only Inventory Service.
 * Uses FHIR Device resource directly via FhirClientService.
 * No local database storage - all data stored in FHIR server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;
    private final OrderService orderService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Extension URLs
    private static final String EXT_CATEGORY = "http://ciyex.com/fhir/StructureDefinition/category";
    private static final String EXT_LOT = "http://ciyex.com/fhir/StructureDefinition/lot";
    private static final String EXT_EXPIRY = "http://ciyex.com/fhir/StructureDefinition/expiry";
    private static final String EXT_SKU = "http://ciyex.com/fhir/StructureDefinition/sku";
    private static final String EXT_STOCK = "http://ciyex.com/fhir/StructureDefinition/stock";
    private static final String EXT_UNIT = "http://ciyex.com/fhir/StructureDefinition/unit";
    private static final String EXT_MIN_STOCK = "http://ciyex.com/fhir/StructureDefinition/min-stock";
    private static final String EXT_LOCATION = "http://ciyex.com/fhir/StructureDefinition/location";
    private static final String EXT_STATUS = "http://ciyex.com/fhir/StructureDefinition/status";
    private static final String EXT_SUPPLIER = "http://ciyex.com/fhir/StructureDefinition/supplier";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public InventoryDto create(InventoryDto dto) {
        validateInventoryDto(dto);

        log.debug("Creating FHIR Device (inventory): {}", dto.getName());

        Device device = toFhirDevice(dto);
        var outcome = fhirClientService.create(device, getPracticeId());
        String fhirId = outcome.getId().getIdPart();
        dto.setId(parseFhirIdToLong(fhirId));
        dto.setFhirId(fhirId);

        // Set audit information
        InventoryDto.Audit audit = new InventoryDto.Audit();
        audit.setCreatedDate(java.time.LocalDateTime.now().toString());
        audit.setLastModifiedDate(java.time.LocalDateTime.now().toString());
        dto.setAudit(audit);

        log.info("Created FHIR Device (inventory) with id: {}", fhirId);

        return dto;
    }

    // GET BY ID
    public InventoryDto getById(String fhirId) {
        log.debug("Getting FHIR Device (inventory): {}", fhirId);
        Optional<Device> deviceOpt = fhirClientService.readOptional(Device.class, fhirId, getPracticeId());
        if (deviceOpt.isEmpty()) {
            throw new RuntimeException("Inventory item not found with id: " + fhirId);
        }
        return fromFhirDevice(deviceOpt.get());
    }

    // GET ALL
    public List<InventoryDto> getAll() {
        log.debug("Getting all FHIR Devices (inventory)");

        Bundle bundle = fhirClientService.search(Device.class, getPracticeId());
        List<Device> devices = fhirClientService.extractResources(bundle, Device.class);

        return devices.stream()
                .map(this::fromFhirDevice)
                .collect(Collectors.toList());
    }

    // GET ALL (Paginated)
    public Page<InventoryDto> getAll(Pageable pageable) {
        List<InventoryDto> all = getAll();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<InventoryDto> pageContent = all.subList(start, end);
        return new PageImpl<>(pageContent, pageable, all.size());
    }

    // UPDATE
    public InventoryDto update(String fhirId, InventoryDto dto) {
        log.debug("Updating FHIR Device (inventory): {}", fhirId);

        Optional<Device> existingOpt = fhirClientService.readOptional(Device.class, fhirId, getPracticeId());
        if (existingOpt.isEmpty()) {
            throw new RuntimeException("Inventory item not found with id: " + fhirId);
        }

        Device device = toFhirDevice(dto);
        device.setId(fhirId);
        fhirClientService.update(device, getPracticeId());

        dto.setId(parseFhirIdToLong(fhirId));
        dto.setFhirId(fhirId);
        return dto;
    }

    // DELETE
    public void delete(String fhirId) {
        log.debug("Deleting FHIR Device (inventory): {}", fhirId);
        Optional<Device> deviceOpt = fhirClientService.readOptional(Device.class, fhirId, getPracticeId());
        if (deviceOpt.isEmpty()) {
            throw new RuntimeException("Inventory item not found with id: " + fhirId);
        }
        fhirClientService.delete(Device.class, fhirId, getPracticeId());
    }

    // CREATE REORDER
    public OrderDto createReorder(String inventoryId, OrderDto dto) {
        InventoryDto inventory = getById(inventoryId);
        return orderService.createOrder(inventory.getName(), inventory.getCategory(), dto.getStock(), dto.getSupplier());
    }

    // ANALYTICS - Weekly Consumption (simplified for FHIR)
    public List<Map<String, Object>> getWeeklyConsumption() {
        // FHIR doesn't track consumption history natively
        // Return placeholder data structure
        List<String> order = Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
        List<Map<String, Object>> result = new ArrayList<>();
        for (String d : order) {
            result.add(Map.of("day", d, "stock", 0));
        }
        return result;
    }

    // ANALYTICS - Monthly Orders
    public List<Map<String, Object>> getMonthlyOrders() {
        List<MonthlyOrderCountDto> counts = orderService.countOrdersByMonth();
        return counts.stream()
                .map(c -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("month", Month.of(c.getMonth()).getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
                    row.put("value", c.getCount());
                    return row;
                })
                .collect(Collectors.toList());
    }

    // COUNT ALL
    public long countAll() {
        return getAll().size();
    }

    // COUNT LOW AND CRITICAL
    public Map<String, Long> countLowAndCritical() {
        List<InventoryDto> items = getAll();

        long low = items.stream()
                .filter(i -> i.getStock() != null && i.getMinStock() != null
                        && i.getStock() <= i.getMinStock() && i.getStock() > 0)
                .count();

        long critical = items.stream()
                .filter(i -> i.getStock() != null && i.getStock() <= 0)
                .count();

        return Map.of("low", low, "critical", critical);
    }

    // -------- FHIR Mapping --------

    private Device toFhirDevice(InventoryDto dto) {
        Device d = new Device();

        // Device name
        if (dto.getName() != null) {
            d.addDeviceName().setName(dto.getName()).setType(Device.DeviceNameType.USERFRIENDLYNAME);
        }

        // Extensions for all custom fields
        addStringExtension(d, EXT_CATEGORY, dto.getCategory());
        addStringExtension(d, EXT_LOT, dto.getLot());
        addStringExtension(d, EXT_EXPIRY, dto.getExpiry());
        addStringExtension(d, EXT_SKU, dto.getSku());
        addStringExtension(d, EXT_UNIT, dto.getUnit());
        addStringExtension(d, EXT_LOCATION, dto.getLocation());
        addStringExtension(d, EXT_STATUS, dto.getStatus());
        addStringExtension(d, EXT_SUPPLIER, dto.getSupplier());

        if (dto.getStock() != null) {
            d.addExtension(new Extension(EXT_STOCK, new IntegerType(dto.getStock())));
        }
        if (dto.getMinStock() != null) {
            d.addExtension(new Extension(EXT_MIN_STOCK, new IntegerType(dto.getMinStock())));
        }

        return d;
    }

    private InventoryDto fromFhirDevice(Device d) {
        InventoryDto dto = new InventoryDto();
        String fhirId = d.getIdElement().getIdPart();
        dto.setId(parseFhirIdToLong(fhirId));
        dto.setFhirId(fhirId);

        // Device name
        if (d.hasDeviceName()) {
            dto.setName(d.getDeviceNameFirstRep().getName());
        }

        // Extensions
        dto.setCategory(getExtensionString(d, EXT_CATEGORY));
        dto.setLot(getExtensionString(d, EXT_LOT));
        dto.setExpiry(getExtensionString(d, EXT_EXPIRY));
        dto.setSku(getExtensionString(d, EXT_SKU));
        dto.setUnit(getExtensionString(d, EXT_UNIT));
        dto.setLocation(getExtensionString(d, EXT_LOCATION));
        dto.setStatus(getExtensionString(d, EXT_STATUS));
        dto.setSupplier(getExtensionString(d, EXT_SUPPLIER));

        Extension stockExt = d.getExtensionByUrl(EXT_STOCK);
        if (stockExt != null && stockExt.getValue() instanceof IntegerType) {
            dto.setStock(((IntegerType) stockExt.getValue()).getValue());
        }

        Extension minStockExt = d.getExtensionByUrl(EXT_MIN_STOCK);
        if (minStockExt != null && minStockExt.getValue() instanceof IntegerType) {
            dto.setMinStock(((IntegerType) minStockExt.getValue()).getValue());
        }

        // Set audit information
        InventoryDto.Audit audit = new InventoryDto.Audit();
        audit.setCreatedDate(java.time.LocalDateTime.now().toString());
        audit.setLastModifiedDate(java.time.LocalDateTime.now().toString());
        dto.setAudit(audit);

        return dto;
    }

    // -------- Helpers --------

    private void addStringExtension(Device d, String url, String value) {
        if (value != null) {
            d.addExtension(new Extension(url, new StringType(value)));
        }
    }

    private String getExtensionString(Device d, String url) {
        Extension ext = d.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) {
            return ((StringType) ext.getValue()).getValue();
        }
        return null;
    }

    private void validateInventoryDto(InventoryDto dto) {
        List<String> errors = new ArrayList<>();

        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            errors.add("Name is mandatory");
        }
        if (dto.getCategory() == null || dto.getCategory().trim().isEmpty()) {
            errors.add("Category is mandatory");
        }
        if (dto.getLot() == null || dto.getLot().trim().isEmpty()) {
            errors.add("Lot is mandatory");
        }
        if (dto.getSku() == null || dto.getSku().trim().isEmpty()) {
            errors.add("SKU is mandatory");
        }
        if (dto.getStock() == null) {
            errors.add("Stock is mandatory");
        } else if (dto.getStock() < 0) {
            errors.add("Stock cannot be negative");
        }
        if (dto.getUnit() == null || dto.getUnit().trim().isEmpty()) {
            errors.add("Unit is mandatory");
        }
        if (dto.getMinStock() == null) {
            errors.add("Min stock is mandatory");
        } else if (dto.getMinStock() < 0) {
            errors.add("Min stock cannot be negative");
        }
        if (dto.getLocation() == null || dto.getLocation().trim().isEmpty()) {
            errors.add("Location is mandatory");
        }
        if (dto.getSupplier() == null || dto.getSupplier().trim().isEmpty()) {
            errors.add("Supplier is mandatory");
        }

        if (!errors.isEmpty()) {
            String errorMessage = "Validation failed: " + String.join(", ", errors);
            log.error("Inventory validation failed: {}", errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private Long parseFhirIdToLong(String fhirId) {
        if (fhirId == null) return null;
        try {
            return Long.parseLong(fhirId);
        } catch (NumberFormatException e) {
            log.warn("FHIR ID '{}' is not numeric, using hashCode", fhirId);
            return (long) Math.abs(fhirId.hashCode());
        }
    }
}
