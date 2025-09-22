package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.OrderDto;
import java.util.List;

public interface ExternalOrderStorage extends ExternalStorage<OrderDto> {
    String createOrder(OrderDto dto);
    void updateOrder(OrderDto dto, String externalId);
    OrderDto getOrder(String externalId);
    void deleteOrder(String externalId);
    List<OrderDto> searchAllOrders();

    @Override default String create(OrderDto entityDto) { return createOrder(entityDto); }
    @Override default void update(OrderDto entityDto, String externalId) { updateOrder(entityDto, externalId); }
    @Override default OrderDto get(String externalId) { return getOrder(externalId); }
    @Override default void delete(String externalId) { deleteOrder(externalId); }
    @Override default List<OrderDto> searchAll() { return searchAllOrders(); }
    @Override default boolean supports(Class<?> entityType) { return OrderDto.class.isAssignableFrom(entityType); }
}
