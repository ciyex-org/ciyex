package com.qiaben.ciyex.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component("noOpExternalStorage")
@Slf4j
public class NoOpExternalStorage<T> implements ExternalStorage<T> {

    @Override
    public String create(T entityDto) {
        log.info("No-op: Would create entityDto: {} but no storage configured", entityDto);
        return null; // No external ID generated
    }

    @Override
    public void update(T entityDto, String externalId) {
        log.info("No-op: Would update entityDto: {} with externalId: {} but no storage configured", entityDto, externalId);
    }

    @Override
    public T get(String externalId) {
        log.info("No-op: Would get entity with externalId: {} but no storage configured", externalId);
        return null; // No entity retrieved
    }

    @Override
    public void delete(String externalId) {
        log.info("No-op: Would delete entity with externalId: {} but no storage configured", externalId);
    }

    @Override
    public List<T> searchAll() {
        log.info("No-op: Would search all entities but no storage configured");
        return Collections.emptyList(); // Return empty list
    }

    @Override
    public boolean supports(Class<?> entityType) {
        log.debug("No-op storage supports any entity type by default");
        return true; // No-op supports all types as a fallback
    }
}