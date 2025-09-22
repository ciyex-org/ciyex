package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.InventoryDto;

import java.util.List;

public interface ExternalInventoryStorage extends ExternalStorage<InventoryDto> {
    String createInventory(InventoryDto dto);
    void updateInventory(InventoryDto dto, String externalId);
    InventoryDto getInventory(String externalId);
    void deleteInventory(String externalId);
    List<InventoryDto> searchAllInventory();

    // Bridge to generic methods
    @Override default String create(InventoryDto entityDto) { return createInventory(entityDto); }
    @Override default void update(InventoryDto entityDto, String externalId) { updateInventory(entityDto, externalId); }
    @Override default InventoryDto get(String externalId) { return getInventory(externalId); }
    @Override default void delete(String externalId) { deleteInventory(externalId); }
    @Override default List<InventoryDto> searchAll() { return searchAllInventory(); }
    @Override default boolean supports(Class<?> entityType) { return InventoryDto.class.isAssignableFrom(entityType); }
}
