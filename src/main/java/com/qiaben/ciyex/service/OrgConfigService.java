package com.qiaben.ciyex.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiaben.ciyex.entity.OrgConfig;
import com.qiaben.ciyex.repository.OrgConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing organization configurations (simple key-value pairs)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgConfigService {

    private final OrgConfigRepository orgConfigRepository;
    private final ObjectMapper objectMapper;

    /**
     * Set a configuration value (create or update)
     */
    @Transactional
    public OrgConfig setConfig(String key, String value) {
        log.info("setConfig(key='{}') called", key);
        
        // Validation: key must not be null or empty
        if (key == null || key.trim().isEmpty()) {
            log.error("Validation failed: key is null or empty");
            throw new IllegalArgumentException("Configuration key cannot be null or empty");
        }
        
        // Validation: key length check
        if (key.length() > 500) {
            log.error("Validation failed: key '{}' exceeds maximum length of 500 characters (actual: {})", 
                     key.substring(0, 50) + "...", key.length());
            throw new IllegalArgumentException("Configuration key exceeds maximum length of 500 characters (actual: " + key.length() + ")");
        }
        
        // Note: Value CAN be null or empty - no validation needed
        
        // Validation: check for invalid key characters
        if (key.contains("..") || key.startsWith(".") || key.endsWith(".")) {
            log.error("Validation failed: key '{}' contains invalid dot notation", key);
            throw new IllegalArgumentException("Configuration key has invalid dot notation: " + key);
        }
        
        Optional<OrgConfig> existing = orgConfigRepository.findByKey(key);
        
        if (existing.isPresent()) {
            OrgConfig config = existing.get();
            config.setValue(value);
            log.info("Updated config key: {} (value: {})", key, value == null ? "null" : "'" + value + "'");
            return orgConfigRepository.save(config);
        } else {
            OrgConfig config = new OrgConfig(key, value);
            log.info("Created config key='{}' (value: {})", key, value == null ? "null" : "'" + value + "'");
            return orgConfigRepository.save(config);
        }
    }

    /**
     * Set multiple configurations at once from a map
     * Keys will be flattened with dot notation for nested objects
     * Example: {"fhir": {"apiUrl": "..."}} becomes "fhir.apiUrl" = "..."
     */
    @Transactional
    public void setConfigBulk(Map<String, Object> configs) {
        log.info("setConfigBulk - root keys: {}", configs == null ? "null" : configs.keySet());
        
        // Validation: configs must not be null or empty
        if (configs == null || configs.isEmpty()) {
            log.error("Validation failed: configs map is null or empty");
            throw new IllegalArgumentException("Configuration map cannot be null or empty");
        }
        
        Map<String, String> flatMap = flattenMap(configs, "");
        log.info("setConfigBulk - flattened {} keys: {}", flatMap.size(), flatMap.keySet());
        
        // Validation: check if flattening produced any keys
        if (flatMap.isEmpty()) {
            log.error("Validation failed: no valid key-value pairs after flattening");
            throw new IllegalArgumentException("No valid configuration key-value pairs found after processing");
        }
        
        // Validate all keys before saving any (values can be null/empty)
        for (Map.Entry<String, String> entry : flatMap.entrySet()) {
            String key = entry.getKey();
            
            if (key == null || key.trim().isEmpty()) {
                log.error("Validation failed: found null or empty key in flattened map");
                throw new IllegalArgumentException("Configuration contains null or empty key after flattening");
            }
            
            if (key.length() > 500) {
                log.error("Validation failed: key '{}...' exceeds maximum length", key.substring(0, 50));
                throw new IllegalArgumentException("Configuration key exceeds maximum length of 500 characters: " + key.substring(0, 50) + "...");
            }
            
            // Note: Values CAN be null or empty - no validation needed
        }
        
        // All validations passed, now save
        for (Map.Entry<String, String> entry : flatMap.entrySet()) {
            setConfig(entry.getKey(), entry.getValue());
        }
        
        log.info("Bulk set {} configuration keys", flatMap.size());
    }

    /**
     * Set configurations from JSON string
     */
    @Transactional
    public void setConfigFromJson(String jsonConfig) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(jsonConfig, Map.class);
            setConfigBulk(map);
        } catch (Exception e) {
            log.error("Failed to parse JSON config", e);
            throw new IllegalArgumentException("Invalid JSON configuration", e);
        }
    }

    /**
     * Get configuration value by key
     */
    public Optional<String> getConfig(String key) {
        return orgConfigRepository.findByKey(key).map(OrgConfig::getValue);
    }

    /**
     * Get configuration entity by key
     */
    public Optional<OrgConfig> getConfigEntity(String key) {
        return orgConfigRepository.findByKey(key);
    }

    /**
     * Get all configurations
     */
    public List<OrgConfig> getAllConfigs() {
        return orgConfigRepository.findAll();
    }

    /**
     * Get all configurations as a Map
     */
    public Map<String, String> getAllConfigsAsMap() {
        return orgConfigRepository.findAll().stream()
                .collect(Collectors.toMap(OrgConfig::getKey, OrgConfig::getValue));
    }

  

    /**
     * Delete configuration by key
     */
    @Transactional
    public void deleteConfig(String key) {
        if (!orgConfigRepository.existsByKey(key)) {
            throw new IllegalArgumentException("Config key not found: " + key);
        }
        orgConfigRepository.deleteByKey(key);
        log.info("Deleted config key: {}", key);
    }

    

    /**
     * Check if configuration key exists
     */
    public boolean existsByKey(String key) {
        return orgConfigRepository.existsByKey(key);
    }

    /**
     * Update configuration value
     */
    @Transactional
    public OrgConfig updateConfig(String key, String value) {
        return setConfig(key, value); // Same as set for key-value store
    }

    // ===== Helper Methods =====

    /**
     * Flatten nested map to dot notation
     * {"fhir": {"apiUrl": "..."}} -> {"fhir.apiUrl": "..."}
     */
    private Map<String, String> flattenMap(Map<String, Object> map, String prefix) {
        Map<String, String> result = new HashMap<>();
        
        if (map == null) {
            return result;
        }
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String entryKey = entry.getKey();
            Object value = entry.getValue();
            
            // Skip null or empty keys
            if (entryKey == null || entryKey.trim().isEmpty()) {
                log.warn("Skipping null or empty key in map");
                continue;
            }
            
            String key = prefix.isEmpty() ? entryKey : prefix + "." + entryKey;
            
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                result.putAll(flattenMap(nestedMap, key));
            } else {
                // Values can be null or empty - store them as-is
                result.put(key, value == null ? null : value.toString());
            }
        }
        
        return result;
    }

    /**
     * Unflatten dot notation to nested map
     * {"fhir.apiUrl": "..."} -> {"fhir": {"apiUrl": "..."}}
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> unflattenMap(Map<String, String> flatMap) {
        Map<String, Object> result = new HashMap<>();
        
        for (Map.Entry<String, String> entry : flatMap.entrySet()) {
            String[] keys = entry.getKey().split("\\.");
            Map<String, Object> current = result;
            
            for (int i = 0; i < keys.length - 1; i++) {
                current = (Map<String, Object>) current.computeIfAbsent(keys[i], k -> new HashMap<>());
            }
            
            current.put(keys[keys.length - 1], entry.getValue());
        }
        
        return result;
    }
}
