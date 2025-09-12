package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.InventoryDto;
import com.qiaben.ciyex.dto.MonthlyOrderCountDto;
import com.qiaben.ciyex.dto.OrderDto;
import com.qiaben.ciyex.entity.Inventory;
import com.qiaben.ciyex.repository.InventoryRepository;
import com.qiaben.ciyex.storage.ExternalInventoryStorage;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import com.qiaben.ciyex.dto.integration.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

@Service
@Slf4j
public class InventoryService {

    private final InventoryRepository repository;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;
    private final OrderService orderService;


    public InventoryService(InventoryRepository repository,
                            ExternalStorageResolver storageResolver,
                            OrgIntegrationConfigProvider configProvider,
                            OrderService orderService) {
        this.repository = repository;
        this.storageResolver = storageResolver;
        this.configProvider = configProvider;
        this.orderService = orderService;
    }


    @Transactional
    public InventoryDto create(InventoryDto dto) {
        Long orgId = getCurrentOrgId();
        dto.setOrgId(orgId);

        Inventory entity = mapToEntity(dto);
        entity.setCreatedDate(now());
        entity.setLastModifiedDate(now());

        // External sync (optional, based on org configuration)
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            ExternalStorage<InventoryDto> storage = storageResolver.resolve(InventoryDto.class);
            String externalId = storage.create(dto);
            entity.setExternalId(externalId);
        }

        return mapToDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public InventoryDto getById(Long id) {
        Inventory entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory item not found"));
        return mapToDto(entity);
    }

    @Transactional
    public InventoryDto update(Long id, InventoryDto dto) {
        Inventory entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory item not found"));

        entity.setSupplier(dto.getSupplier());
        entity.setName(dto.getName());
        entity.setCategory(dto.getCategory());
        entity.setLot(dto.getLot());
        entity.setExpiry(dto.getExpiry());
        entity.setSku(dto.getSku());
        entity.setStock(dto.getStock());
        entity.setUnit(dto.getUnit());
        entity.setMinStock(dto.getMinStock());
        entity.setLocation(dto.getLocation());
        entity.setStatus(dto.getStatus());
        entity.setLastModifiedDate(now());

        // External sync (if configured & item linked)
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && entity.getExternalId() != null) {
            ExternalStorage<InventoryDto> storage = storageResolver.resolve(InventoryDto.class);
            storage.update(mapToDto(entity), entity.getExternalId());
        }

        return mapToDto(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        Inventory entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory item not found"));

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && entity.getExternalId() != null) {
            ExternalStorage<InventoryDto> storage = storageResolver.resolve(InventoryDto.class);
            storage.delete(entity.getExternalId());
        }

        repository.delete(entity);
    }

    @Transactional(readOnly = true)
    public List<InventoryDto> getAll() {
        return repository.findByOrgId(getCurrentOrgId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<InventoryDto> getAll(Pageable pageable) {
        Long orgId = getCurrentOrgId();
        return repository.findAllByOrgId(orgId, pageable).map(this::mapToDto);
    }

    @Transactional
    public OrderDto createReorder(Long inventoryId, OrderDto dto) {
        Inventory entity = repository.findById(inventoryId)
                .orElseThrow(() -> new RuntimeException("Inventory item not found"));

        // use dto.getStock() instead of getQuantity()
        return orderService.createOrder(entity, dto.getStock(), dto.getSupplier());
    }


    @Transactional(readOnly = true)
    public List<Map<String, Object>> getWeeklyConsumption() {
        Long orgId = getCurrentOrgId();
        List<Inventory> items = repository.findByOrgId(orgId);

        Map<String, Integer> grouped = items.stream()
                .filter(i -> i.getLastModifiedDate() != null)  // use entity field, not audit
                .collect(Collectors.groupingBy(
                        i -> {
                            LocalDate date = LocalDate.parse(i.getLastModifiedDate().substring(0, 10));
                            return date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                        },
                        Collectors.summingInt(Inventory::getStock)
                ));

        List<String> order = Arrays.asList("Mon","Tue","Wed","Thu","Fri","Sat","Sun");
        List<Map<String, Object>> result = new ArrayList<>();
        for (String d : order) {
            result.add(Map.of(
                    "day", d,
                    "stock", grouped.getOrDefault(d, 0)
            ));
        }
        return result;
    }


    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMonthlyOrders() {
        Long orgId = getCurrentOrgId();
        List<MonthlyOrderCountDto> counts = orderService.countOrdersByMonth(orgId);

        return counts.stream()
                .map(c -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("month", Month.of(c.getMonth()).getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
                    row.put("value", c.getCount());
                    return row;
                })
                .collect(Collectors.toList());

    }


    @Transactional(readOnly = true)
    public long countAll() {
        return repository.countByOrgId(getCurrentOrgId());
    }

    @Transactional(readOnly = true)
    public Map<String, Long> countLowAndCritical() {
        Long orgId = getCurrentOrgId();
        List<Inventory> items = repository.findByOrgId(orgId);

        long low = items.stream()
                .filter(i -> i.getStock() != null && i.getMinStock() != null
                        && i.getStock() <= i.getMinStock() && i.getStock() > 0)
                .count();

        long critical = items.stream()
                .filter(i -> i.getStock() != null && i.getStock() <= 0)
                .count();

        return Map.of("low", low, "critical", critical);
    }





    private Inventory mapToEntity(InventoryDto dto) {
        return Inventory.builder()
                .id(dto.getId())
                .orgId(dto.getOrgId())
                .name(dto.getName())
                .category(dto.getCategory())
                .lot(dto.getLot())
                .expiry(dto.getExpiry())
                .sku(dto.getSku())
                .stock(dto.getStock())
                .supplier(dto.getSupplier())
                .unit(dto.getUnit())
                .minStock(dto.getMinStock())
                .location(dto.getLocation())
                .status(dto.getStatus())
                .build();
    }

    private InventoryDto mapToDto(Inventory e) {
        InventoryDto dto = new InventoryDto();
        dto.setId(e.getId());
        dto.setOrgId(e.getOrgId());
        dto.setName(e.getName());
        dto.setCategory(e.getCategory());
        dto.setLot(e.getLot());
        dto.setExpiry(e.getExpiry());
        dto.setSku(e.getSku());
        dto.setStock(e.getStock());
        dto.setUnit(e.getUnit());
        dto.setMinStock(e.getMinStock());
        dto.setLocation(e.getLocation());
        dto.setStatus(e.getStatus());
        dto.setSupplier(e.getSupplier());
        dto.setFhirId(e.getExternalId());

        InventoryDto.Audit audit = new InventoryDto.Audit();
        audit.setCreatedDate(e.getCreatedDate());
        audit.setLastModifiedDate(e.getLastModifiedDate());
        dto.setAudit(audit);

        return dto;
    }

    private Long getCurrentOrgId() {
        return RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
    }

    private String now() {
        return LocalDateTime.now().toString();
    }
}
