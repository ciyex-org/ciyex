package com.qiaben.ciyex.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IntegrationConfigMapper {
    private final ObjectMapper objectMapper;

    @Autowired
    public IntegrationConfigMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> T mapIntegration(JsonNode integrations, String key, Class<T> clazz) {
        JsonNode section = integrations.get(key);
        if (section == null || section.isNull()) return null;
        try {
            return objectMapper.treeToValue(section, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to map integration config", e);
        }
    }
}
