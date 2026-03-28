package org.ciyex.ehr.tabconfig.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.tabconfig.entity.TabConfig;
import org.ciyex.ehr.tabconfig.entity.TabFieldConfig;
import org.ciyex.ehr.tabconfig.repository.TabConfigRepository;
import org.ciyex.ehr.tabconfig.repository.TabFieldConfigRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TabFieldConfigService {

    private final TabFieldConfigRepository repository;
    private final TabConfigRepository tabConfigRepository;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Get effective field config for a tab, resolving the 3-level fallback:
     * org-specific → practice-type-specific → universal (*)
     */
    public TabFieldConfig getEffectiveFieldConfig(String tabKey, String practiceTypeCode, String orgId) {
        List<TabFieldConfig> results = repository.findEffective(tabKey, practiceTypeCode != null ? practiceTypeCode : "*", orgId);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Get field config for a specific practice type default (no org override).
     */
    public TabFieldConfig getPracticeTypeFieldConfig(String tabKey, String practiceTypeCode) {
        return repository.findByTabKeyAndPracticeTypeCodeAndOrgId(tabKey, practiceTypeCode, "*")
                .orElse(null);
    }

    /**
     * List all tab field configs for a practice type (with universal fallbacks).
     */
    public List<TabFieldConfig> listFieldConfigsForPracticeType(String practiceTypeCode, String orgId) {
        List<TabFieldConfig> all = repository.findAllForPracticeTypeAndOrg(practiceTypeCode, orgId);

        // Deduplicate: for each tabKey, keep the most specific config
        Map<String, TabFieldConfig> byTab = new LinkedHashMap<>();
        for (TabFieldConfig cfg : all) {
            String key = cfg.getTabKey();
            TabFieldConfig existing = byTab.get(key);
            if (existing == null || isMoreSpecific(cfg, existing)) {
                byTab.put(key, cfg);
            }
        }
        return new ArrayList<>(byTab.values());
    }

    /**
     * List all available tab keys — automatically derived from chart tab_config
     * plus any that have field config entries (universal + org-specific).
     */
    public List<Map<String, Object>> listAvailableTabs() {
        String orgId = resolveOrgId();
        // Collect all tab keys from chart config (universal default)
        Set<String> allTabKeys = new LinkedHashSet<>();
        tabConfigRepository.findByOrgId("*").ifPresent(tc -> {
            try {
                JsonNode categories = objectMapper.readTree(tc.getTabConfig());
                if (categories.isArray()) {
                    for (JsonNode cat : categories) {
                        JsonNode tabs = cat.get("tabs");
                        if (tabs != null && tabs.isArray()) {
                            for (JsonNode tab : tabs) {
                                String key = tab.has("key") ? tab.get("key").asText() : null;
                                if (key != null) allTabKeys.add(key);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse tab_config for available tabs", e);
            }
        });

        // Include universal field config entries
        List<String> fieldConfigKeys = repository.findAllUniversalTabKeys();
        allTabKeys.addAll(fieldConfigKeys);

        // Include org-specific field config entries (e.g. custom FHIR tabs added via Menu Configuration)
        if (orgId != null && !"*".equals(orgId)) {
            for (TabFieldConfig cfg : repository.findByOrgId(orgId)) {
                allTabKeys.add(cfg.getTabKey());
            }
        }

        // Build result with FHIR resources — prefer org-specific, fallback to universal
        List<Map<String, Object>> result = new ArrayList<>();
        for (String tabKey : allTabKeys) {
            Map<String, Object> tabInfo = new LinkedHashMap<>();
            tabInfo.put("tabKey", tabKey);
            TabFieldConfig cfg = null;
            if (orgId != null && !"*".equals(orgId)) {
                cfg = repository.findByTabKeyAndPracticeTypeCodeAndOrgId(tabKey, "*", orgId).orElse(null);
            }
            if (cfg == null) {
                cfg = repository.findByTabKeyAndPracticeTypeCodeAndOrgId(tabKey, "*", "*").orElse(null);
            }
            tabInfo.put("fhirResources", cfg != null ? parseJson(cfg.getFhirResources()) : List.of());
            result.add(tabInfo);
        }
        return result;
    }

    /**
     * Set app.current_org in the DB session so RLS allows org-specific row access.
     * Must be called within a @Transactional method so the setting applies to the
     * same connection used by JPA operations.
     */
    private void setOrgContext(String orgId) {
        if (orgId != null && !orgId.isBlank() && !"*".equals(orgId)) {
            jdbcTemplate.queryForObject("SELECT set_config('app.current_org', ?, true)", String.class, orgId);
        }
    }

    /**
     * Save org-specific field config override for a tab.
     */
    @Transactional
    public TabFieldConfig saveOrgFieldConfig(String tabKey, String orgId, String practiceTypeCode, String fhirResources, String fieldConfig, String apiBasePath, String category) {
        setOrgContext(orgId);
        Optional<TabFieldConfig> existing = repository.findByTabKeyAndPracticeTypeCodeAndOrgId(tabKey, practiceTypeCode != null ? practiceTypeCode : "*", orgId);

        TabFieldConfig cfg;
        if (existing.isPresent()) {
            cfg = existing.get();
            cfg.setFieldConfig(fieldConfig);
            if (fhirResources != null) cfg.setFhirResources(fhirResources);
            if (apiBasePath != null) cfg.setApiBasePath(apiBasePath);
            if (category != null) cfg.setCategory(category);
            cfg.setVersion(cfg.getVersion() + 1);
        } else {
            cfg = new TabFieldConfig();
            cfg.setTabKey(tabKey);
            cfg.setPracticeTypeCode(practiceTypeCode != null ? practiceTypeCode : "*");
            cfg.setOrgId(orgId);
            cfg.setFhirResources(fhirResources != null ? fhirResources : "[]");
            cfg.setFieldConfig(fieldConfig);
            cfg.setApiBasePath(apiBasePath);
            if (category != null) cfg.setCategory(category);
        }
        return repository.save(cfg);
    }

    /**
     * Delete org-specific override (reverts to practice-type or universal default).
     */
    @Transactional
    public void resetOrgFieldConfig(String tabKey, String orgId) {
        setOrgContext(orgId);
        repository.deleteByTabKeyAndOrgId(tabKey, orgId);
    }

    // ==================== Layout (unified tab_config) ====================

    /**
     * Get the tab layout for an org — replaces TabConfigService.getEffectiveConfig().
     * Returns categories with tabs, each including FHIR resource info.
     */
    public Map<String, Object> getLayout(String orgId) {
        List<TabFieldConfig> all = repository.findAllForLayout(orgId, "*");

        // Deduplicate by tabKey: org-specific beats universal
        Map<String, TabFieldConfig> byTab = new LinkedHashMap<>();
        for (TabFieldConfig cfg : all) {
            String key = cfg.getTabKey();
            TabFieldConfig existing = byTab.get(key);
            if (existing == null || isMoreSpecific(cfg, existing)) {
                byTab.put(key, cfg);
            }
        }

        // Group by category, preserving categoryPosition order (exclude Settings — they use the Settings page, not the Chart)
        Map<String, List<TabFieldConfig>> grouped = byTab.values().stream()
                .filter(c -> c.getCategory() != null && !"Settings".equals(c.getCategory()))
                .sorted(Comparator.comparingInt(c -> c.getCategoryPosition() != null ? c.getCategoryPosition() : 0))
                .collect(Collectors.groupingBy(
                        TabFieldConfig::getCategory,
                        LinkedHashMap::new,
                        Collectors.toList()));

        // Build response matching the existing tabConfig format
        List<Map<String, Object>> categories = new ArrayList<>();
        int catPosition = 0;
        for (Map.Entry<String, List<TabFieldConfig>> entry : grouped.entrySet()) {
            List<TabFieldConfig> tabs = entry.getValue();
            tabs.sort(Comparator.comparingInt(t -> t.getPosition() != null ? t.getPosition() : 0));

            List<Map<String, Object>> tabItems = new ArrayList<>();
            for (TabFieldConfig tab : tabs) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("key", tab.getTabKey());
                item.put("label", tab.getLabel() != null ? tab.getLabel() : tab.getTabKey());
                item.put("icon", tab.getIcon() != null ? tab.getIcon() : "FileText");
                item.put("visible", tab.getVisible() != null ? tab.getVisible() : true);
                item.put("position", tab.getPosition() != null ? tab.getPosition() : 0);
                item.put("fhirResources", parseJson(tab.getFhirResources()));
                tabItems.add(item);
            }

            Map<String, Object> cat = new LinkedHashMap<>();
            cat.put("label", entry.getKey());
            cat.put("position", catPosition++);
            cat.put("tabs", tabItems);
            categories.add(cat);
        }

        // Determine source
        boolean hasOrgOverrides = byTab.values().stream().anyMatch(c -> !"*".equals(c.getOrgId()));
        String source = hasOrgOverrides ? "ORG_CUSTOM" : "UNIVERSAL_DEFAULT";

        return Map.of(
                "tabConfig", categories,
                "source", source
        );
    }

    /**
     * Save the tab layout for an org (bulk update of layout columns).
     * Accepts the same format as getLayout() returns.
     */
    @Transactional
    public void saveLayout(String orgId, String layoutJson) {
        setOrgContext(orgId);
        try {
            JsonNode categories = objectMapper.readTree(layoutJson);
            if (!categories.isArray()) return;

            int catIdx = 0;
            for (JsonNode cat : categories) {
                String categoryLabel = cat.has("label") ? cat.get("label").asText() : "Other";
                int categoryPosition = cat.has("position") ? cat.get("position").asInt() : catIdx;
                JsonNode tabs = cat.get("tabs");
                if (tabs == null || !tabs.isArray()) { catIdx++; continue; }

                for (JsonNode tab : tabs) {
                    String tabKey = tab.has("key") ? tab.get("key").asText() : null;
                    if (tabKey == null) continue;

                    String label = tab.has("label") ? tab.get("label").asText() : tabKey;
                    String icon = tab.has("icon") ? tab.get("icon").asText() : "FileText";
                    boolean visible = !tab.has("visible") || tab.get("visible").asBoolean(true);
                    int position = tab.has("position") ? tab.get("position").asInt() : 0;

                    // Read fhirResources from payload if present
                    String fhirResourcesJson = null;
                    if (tab.has("fhirResources") && tab.get("fhirResources").isArray()) {
                        fhirResourcesJson = objectMapper.writeValueAsString(tab.get("fhirResources"));
                    }

                    // Read apiBasePath from payload if present
                    String apiBasePath = tab.has("apiBasePath") && !tab.get("apiBasePath").isNull()
                            ? tab.get("apiBasePath").asText() : null;

                    // Find or create org-specific override
                    Optional<TabFieldConfig> existing = repository.findByTabKeyAndPracticeTypeCodeAndOrgId(tabKey, "*", orgId);
                    TabFieldConfig cfg;
                    if (existing.isPresent()) {
                        cfg = existing.get();
                    } else {
                        // Create org override — copy fhir_resources and field_config from universal
                        cfg = new TabFieldConfig();
                        cfg.setTabKey(tabKey);
                        cfg.setPracticeTypeCode("*");
                        cfg.setOrgId(orgId);
                        TabFieldConfig universal = repository.findByTabKeyAndPracticeTypeCodeAndOrgId(tabKey, "*", "*").orElse(null);
                        cfg.setFhirResources(universal != null ? universal.getFhirResources() : "[]");
                        cfg.setFieldConfig(universal != null ? universal.getFieldConfig() : "{}");
                        cfg.setApiBasePath(universal != null ? universal.getApiBasePath() : null);
                    }
                    // Update fhirResources if provided in payload
                    if (fhirResourcesJson != null) {
                        cfg.setFhirResources(fhirResourcesJson);
                    }
                    if (apiBasePath != null) {
                        cfg.setApiBasePath(apiBasePath);
                    }
                    cfg.setLabel(label);
                    cfg.setIcon(icon);
                    cfg.setCategory(categoryLabel);
                    cfg.setCategoryPosition(categoryPosition);
                    cfg.setPosition(position);
                    cfg.setVisible(visible);
                    repository.save(cfg);
                }
                catIdx++;
            }
        } catch (Exception e) {
            log.error("Failed to save layout for org: {}", orgId, e);
            throw new RuntimeException("Failed to save tab layout", e);
        }
    }

    /**
     * Reset org layout overrides (revert to universal defaults).
     */
    @Transactional
    public void resetLayout(String orgId) {
        setOrgContext(orgId);
        // Only delete org-specific rows (where label is set = layout override)
        List<TabFieldConfig> orgConfigs = repository.findByOrgId(orgId);
        for (TabFieldConfig cfg : orgConfigs) {
            if (!"*".equals(cfg.getOrgId())) {
                repository.delete(cfg);
            }
        }
    }

    private boolean isMoreSpecific(TabFieldConfig a, TabFieldConfig b) {
        // org-specific beats global
        if (!"*".equals(a.getOrgId()) && "*".equals(b.getOrgId())) return true;
        if ("*".equals(a.getOrgId()) && !"*".equals(b.getOrgId())) return false;
        // practice-type-specific beats universal
        if (!"*".equals(a.getPracticeTypeCode()) && "*".equals(b.getPracticeTypeCode())) return true;
        return false;
    }

    private String resolveOrgId() {
        try {
            RequestContext ctx = RequestContext.get();
            return ctx != null && ctx.getOrgName() != null ? ctx.getOrgName() : "*";
        } catch (Exception e) {
            return "*";
        }
    }

    private Object parseJson(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }
}
