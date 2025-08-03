package com.qiaben.ciyex.storage;

import java.util.List;

public interface ExternalStorage<T> {
    String create(T entityDto);
    void update(T entityDto, String externalId);
    T get(String externalId);
    void delete(String externalId);
    List<T> searchAll();
    boolean supports(Class<?> entityType); // Check if this storage supports the entity
}