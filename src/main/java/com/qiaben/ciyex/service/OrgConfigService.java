package com.qiaben.ciyex.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * FHIR-only Organization Configuration Service.
 * Uses FHIR Basic resource for storing key-value configuration pairs.
 * No local database storage - all data stored in FHIR server.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgConfigService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;
    private final ObjectMapper objectMapper;

    private static final String CONFIG_TYPE_SYSTEM = "http://ciyex.com/fhir/resource-type";
    private static final String CONFIG_TYPE_CODE = "org-config";
    private static final String EXT_CONFIG_KEY = "http://ciyex.com/fhir/StructureDefinition/config-key";
    private static final String EXT_CONFIG_VALUE = "http://ciyex.com/fhir/StructureDefinition/config-value";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    /**
     * Set a configuration value (create or update)
     */
    public OrgConfigResult setConfig(String key, String value) {
        log.info("setConfig(key='{}') called", key);

        // Validation
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Configuration key cannot be null or empty");
        }
        if (key.length() > 500) {
            throw new IllegalArgumentException("Configuration key exceeds maximum length of 500 characters");
        }
        if (key.contains("..") || key.startsWith(".") || key.endsWith(".")) {
            throw new IllegalArgumentException("Configuration key has invalid dot notation: " + key);
        }

        // Check if config exists
        Optional<Basic> existing = findConfigByKey(key);

        if (existing.isPresent()) {
            throw new IllegalArgumentException("The key '" + key + "' is already available");
        } else {
            // Create new
            Basic basic = new Basic();

            CodeableConcept code = new CodeableConcept();
            code.addCoding().setSystem(CONFIG_TYPE_SYSTEM).setCode(CONFIG_TYPE_CODE).setDisplay("Org Config");
            basic.setCode(code);

            basic.addExtension(new Extension(EXT_CONFIG_KEY, new StringType(key)));
            basic.addExtension(new Extension(EXT_CONFIG_VALUE, new StringType(value)));

            var outcome = fhirClientService.create(basic, getPracticeId());
            String fhirId = outcome.getId().getIdPart();

            log.info("Created config key='{}' with FHIR ID: {}", key, fhirId);
            return new OrgConfigResult(fhirId, key, value);
        }
    }

    /**
     * Set multiple configurations at once from a map
     */
    public void setConfigBulk(Map<String, Object> configs) {
        log.info("setConfigBulk - root keys: {}", configs == null ? "null" : configs.keySet());

        if (configs == null || configs.isEmpty()) {
            throw new IllegalArgumentException("Configuration map cannot be null or empty");
        }

        Map<String, String> flatMap = flattenMap(configs, "");
        log.info("setConfigBulk - flattened {} keys", flatMap.size());

        if (flatMap.isEmpty()) {
            throw new IllegalArgumentException("No valid configuration key-value pairs found after processing");
        }

        for (Map.Entry<String, String> entry : flatMap.entrySet()) {
            setConfig(entry.getKey(), entry.getValue());
        }

        log.info("Bulk set {} configuration keys", flatMap.size());
    }

    /**
     * Set configurations from JSON string
     */
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
        return findConfigByKey(key)
                .map(basic -> {
                    Extension ext = basic.getExtensionByUrl(EXT_CONFIG_VALUE);
                    if (ext != null && ext.getValue() instanceof StringType) {
                        return ((StringType) ext.getValue()).getValue();
                    }
                    return null;
                });
    }

    /**
     * Get configuration entity by key
     */
    public Optional<OrgConfigResult> getConfigEntity(String key) {
        return findConfigByKey(key)
                .map(basic -> {
                    String fhirId = basic.getIdElement().getIdPart();
                    String value = null;
                    Extension ext = basic.getExtensionByUrl(EXT_CONFIG_VALUE);
                    if (ext != null && ext.getValue() instanceof StringType) {
                        value = ((StringType) ext.getValue()).getValue();
                    }
                    return new OrgConfigResult(fhirId, key, value);
                });
    }

    /**
     * Get all configurations
     */
    public List<OrgConfigResult> getAllConfigs() {
        Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());

        return fhirClientService.extractResources(bundle, Basic.class).stream()
                .filter(this::isOrgConfig)
                .map(this::toOrgConfigResult)
                .collect(Collectors.toList());
    }

    /**
     * Get all configurations as a Map
     */
    public Map<String, String> getAllConfigsAsMap() {
        return getAllConfigs().stream()
                .collect(Collectors.toMap(OrgConfigResult::getKey, r -> r.getValue() != null ? r.getValue() : ""));
    }

    /**
     * Delete configuration by key
     */
    public void deleteConfig(String key) {
        Optional<Basic> existing = findConfigByKey(key);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Config key not found: " + key);
        }

        String fhirId = existing.get().getIdElement().getIdPart();
        fhirClientService.delete(Basic.class, fhirId, getPracticeId());
        log.info("Deleted config key: {}", key);
    }

    /**
     * Check if configuration key exists
     */
    public boolean existsByKey(String key) {
        return findConfigByKey(key).isPresent();
    }

    /**
     * Update configuration value
     */
    public OrgConfigResult updateConfig(String key, String value) {
        log.info("updateConfig(key='{}') called", key);

        // Validation
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Configuration key cannot be null or empty");
        }

        // Check if config exists
        Optional<Basic> existing = findConfigByKey(key);

        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Config key not found: " + key);
        }

        // Update existing
        Basic basic = existing.get();
        
        // Update the value extension
        Extension valueExt = basic.getExtensionByUrl(EXT_CONFIG_VALUE);
        if (valueExt != null) {
            valueExt.setValue(new StringType(value));
        } else {
            basic.addExtension(new Extension(EXT_CONFIG_VALUE, new StringType(value)));
        }

        var outcome = fhirClientService.update(basic, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        log.info("Updated config key='{}' with FHIR ID: {}", key, fhirId);
        return new OrgConfigResult(fhirId, key, value);
    }

    /**
     * Public method to flatten map for controller use
     */
    public Map<String, String> flattenMapPublic(Map<String, Object> configs) {
        return flattenMap(configs, "");
    }

    // ===== Helper Methods =====

    private Optional<Basic> findConfigByKey(String key) {
        log.info("Searching for config key: '{}'", key);
        Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());

        List<Basic> allConfigs = fhirClientService.extractResources(bundle, Basic.class).stream()
                .filter(this::isOrgConfig)
                .collect(Collectors.toList());
        
        log.info("Found {} total org-config resources", allConfigs.size());
        
        for (Basic basic : allConfigs) {
            String configKey = getConfigKey(basic);
            String fhirId = basic.getIdElement().getIdPart();
            log.info("Checking resource ID={}, key='{}' against search key='{}'", fhirId, configKey, key);
            if (key.equals(configKey)) {
                log.info("MATCH FOUND for key='{}' with ID={}", key, fhirId);
                return Optional.of(basic);
            }
        }
        
        log.warn("NO MATCH found for key='{}'", key);
        return Optional.empty();
    }

    private boolean isOrgConfig(Basic basic) {
        if (!basic.hasCode()) return false;
        return basic.getCode().getCoding().stream()
                .anyMatch(c -> CONFIG_TYPE_SYSTEM.equals(c.getSystem()) && CONFIG_TYPE_CODE.equals(c.getCode()));
    }

    private String getConfigKey(Basic basic) {
        Extension ext = basic.getExtensionByUrl(EXT_CONFIG_KEY);
        if (ext != null && ext.getValue() instanceof StringType) {
            return ((StringType) ext.getValue()).getValue();
        }
        return null;
    }

    private OrgConfigResult toOrgConfigResult(Basic basic) {
        String fhirId = basic.getIdElement().getIdPart();
        String key = getConfigKey(basic);
        String value = null;
        Extension ext = basic.getExtensionByUrl(EXT_CONFIG_VALUE);
        if (ext != null && ext.getValue() instanceof StringType) {
            value = ((StringType) ext.getValue()).getValue();
        }
        return new OrgConfigResult(fhirId, key, value);
    }

    private Map<String, String> flattenMap(Map<String, Object> map, String prefix) {
        Map<String, String> result = new HashMap<>();

        if (map == null) {
            return result;
        }

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String entryKey = entry.getKey();
            Object value = entry.getValue();

            if (entryKey == null || entryKey.trim().isEmpty()) {
                continue;
            }

            String key = prefix.isEmpty() ? entryKey : prefix + "." + entryKey;

            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                result.putAll(flattenMap(nestedMap, key));
            } else {
                result.put(key, value == null ? null : value.toString());
            }
        }

        return result;
    }

    /**
     * Result class for org config operations (replaces OrgConfig entity)
     */
    public static class OrgConfigResult {
        private final String id;
        private final String key;
        private final String value;

        public OrgConfigResult(String id, String key, String value) {
            this.id = id;
            this.key = key;
            this.value = value;
        }

        public String getId() { return id; }
        public String getKey() { return key; }
        public String getValue() { return value; }
    }
}
