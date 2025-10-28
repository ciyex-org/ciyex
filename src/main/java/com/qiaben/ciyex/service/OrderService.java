package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.MonthlyOrderCountDto;
import com.qiaben.ciyex.dto.OrderDto;
import com.qiaben.ciyex.entity.Inventory;
import com.qiaben.ciyex.entity.Order;
import com.qiaben.ciyex.repository.InventoryRepository;
import com.qiaben.ciyex.repository.OrderRepository;
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
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderService {

    private final OrderRepository repository;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;

    private final InventoryRepository inventoryRepository;


    public OrderService(OrderRepository repository,
                        InventoryRepository inventoryRepository,
                        ExternalStorageResolver storageResolver,
                        OrgIntegrationConfigProvider configProvider) {
        this.repository = repository;
        this.inventoryRepository = inventoryRepository; // now it matches the parameter
        this.storageResolver = storageResolver;
        this.configProvider = configProvider;
    }


    @Transactional
    public OrderDto create(OrderDto dto) {
        
        Order entity = mapToEntity(dto);


        // Generate PO number here if missing
        if (entity.getOrderNumber() == null) {
            entity.setOrderNumber("PO-" + (repository.count() + 1));
        }

        // Now call external storage with a populated orderNumber
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            ExternalStorage<OrderDto> storage = storageResolver.resolve(OrderDto.class);
            String externalId = storage.create(mapToDto(entity));
            entity.setExternalId(externalId);
        }

        return mapToDto(repository.save(entity));
    }


    @Transactional(readOnly = true)
    public OrderDto getById(Long id) {
        Order entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return mapToDto(entity);
    }

    @Transactional
    public OrderDto update(Long id, OrderDto dto) {
        Order entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        entity.setOrderNumber(dto.getOrderNumber());
        entity.setSupplier(dto.getSupplier());
        entity.setDate(dto.getDate());
        entity.setStatus(dto.getStatus());
        entity.setAmount(dto.getAmount());

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && entity.getExternalId() != null) {
            ExternalStorage<OrderDto> storage = storageResolver.resolve(OrderDto.class);
            storage.update(mapToDto(entity), entity.getExternalId());
        }

        return mapToDto(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        Order entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && entity.getExternalId() != null) {
            ExternalStorage<OrderDto> storage = storageResolver.resolve(OrderDto.class);
            storage.delete(entity.getExternalId());
        }

        repository.delete(entity);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getAll() {
        return repository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> getAll(Pageable pageable) {
        
        return repository.findAll(pageable).map(this::mapToDto);
    }

    @Transactional
    public OrderDto receiveOrder(Long orderId, OrderDto dto) {
        Order order = repository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!"Pending".equals(order.getStatus()) && !"Received".equals(order.getStatus())) {
            throw new RuntimeException("Only pending orders can be received");
        }

        Inventory item = order.getInventory();

        // ✅ Always update inventory stock with dto.stock (value entered in Receive form)
        if (dto != null && dto.getStock() != null) {
            item.setStock(dto.getStock());
        }

        if (dto != null && dto.getSupplier() != null) {
            item.setSupplier(dto.getSupplier());
        }
        inventoryRepository.save(item);

        // ✅ Update order fields
        order.setStatus("Received");
        order.setStock(dto.getStock()); // keep stock on the order
        order.setDate(LocalDateTime.now().toLocalDate().toString());

        if (dto != null && dto.getAmount() != null) {
            order.setAmount(dto.getAmount());
        }

        return mapToDto(repository.save(order));
    }
    @Transactional
    public OrderDto createOrder(Inventory inventory, Integer stock, String supplier) {
        Order entity = Order.builder()
                .orderNumber("PO-" + (repository.count() + 1))
                .supplier(supplier)
                .date(LocalDateTime.now().toLocalDate().toString())
                .status("Pending")
                .stock(stock)                       // ordered quantity
                .itemName(inventory.getName())      // track item name
                .category(inventory.getCategory())   // ✅ set here
                .amount(0.0)                        // calculate later if needed
                .inventory(inventory)
                .build();

        // ✅ External sync just like create()
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            ExternalStorage<OrderDto> storage = storageResolver.resolve(OrderDto.class);
            String externalId = storage.create(mapToDto(entity));
            entity.setExternalId(externalId);
        }

        return mapToDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<MonthlyOrderCountDto> countOrdersByMonth() {
        return repository.countOrdersByMonth().stream()
                .map(r -> new MonthlyOrderCountDto(((Number) r[0]).intValue(), ((Number) r[1]).longValue()))
                .toList();
    }


    @Transactional(readOnly = true)
    public long countPending() {
        // TODO: Implement countByStatus in repository
        return 0;
    }









    private Order mapToEntity(OrderDto dto) {
        return Order.builder()
                .id(dto.getId())
                .orderNumber(dto.getOrderNumber())
                .supplier(dto.getSupplier())
                .date(dto.getDate())
                .status(dto.getStatus())
                .stock(dto.getStock())
                .itemName(dto.getItemName())
                .category(dto.getCategory())
                .amount(dto.getAmount())
                .build();
    }

    private OrderDto mapToDto(Order e) {
        OrderDto dto = new OrderDto();
        dto.setId(e.getId());
        dto.setOrderNumber(e.getOrderNumber());
        dto.setSupplier(e.getSupplier());
        dto.setDate(e.getDate());
        dto.setStatus(e.getStatus());
        dto.setStock(e.getStock());
        dto.setItemName(
                e.getInventory() != null ? e.getInventory().getName() : e.getItemName()
        );

        dto.setCategory(
                e.getInventory() != null ? e.getInventory().getCategory() : e.getCategory()
        );
        dto.setAmount(e.getAmount());
        dto.setFhirId(e.getExternalId());

        OrderDto.Audit audit = new OrderDto.Audit();
        dto.setAudit(audit);

        return dto;
    }


    

    private String now() {
        return LocalDateTime.now().toString();
    }
}
