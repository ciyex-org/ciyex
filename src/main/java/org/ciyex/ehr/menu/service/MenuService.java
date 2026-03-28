package org.ciyex.ehr.menu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.menu.dto.MenuDetailDto;
import org.ciyex.ehr.menu.entity.Menu;
import org.ciyex.ehr.menu.entity.MenuItem;
import org.ciyex.ehr.menu.entity.MenuOrgOverride;
import org.ciyex.ehr.menu.repository.MenuItemRepository;
import org.ciyex.ehr.menu.repository.MenuOrgOverrideRepository;
import org.ciyex.ehr.menu.repository.MenuRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuService {

    private final MenuRepository menuRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuOrgOverrideRepository overrideRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Get resolved menu by code: practice-type menu (or default) + org overrides applied.
     */
    public MenuDetailDto getMenuByCode(String orgId, String code) {
        return getMenuByCode(orgId, code, "*");
    }

    /**
     * Get resolved menu by code with practice type support.
     * Resolution: practice-type-specific global → default global → apply org overrides.
     */
    public MenuDetailDto getMenuByCode(String orgId, String code, String practiceTypeCode) {
        // Find best matching global menu (practice-type-specific first, then default)
        String ptCode = practiceTypeCode != null ? practiceTypeCode : "*";
        List<Menu> candidates = menuRepository.findGlobalByCodeForPracticeType(code, ptCode);
        if (candidates.isEmpty()) return null;

        Menu globalMenu = candidates.get(0); // Best match (practice-type-specific if exists, else default)

        // Build global menu detail
        MenuDetailDto detail = buildMenuDetail(globalMenu);

        // If org is global itself, just return
        if ("*".equals(orgId)) return detail;

        // Apply org overrides
        List<MenuOrgOverride> overrides = overrideRepository.findByOrgIdAndMenuCode(orgId, code);
        if (overrides.isEmpty()) return detail;

        return applyOverrides(detail, overrides);
    }

    /**
     * Check if org has any customizations for the given menu.
     */
    public boolean hasOrgMenu(String orgId, String code) {
        return overrideRepository.existsByOrgIdAndMenuCode(orgId, code);
    }

    /**
     * Hide a menu item for this org.
     */
    @Transactional
    public void hideItem(String orgId, String code, UUID itemId) {
        setOrgContext(orgId);
        // Check if there's already an override for this item
        Optional<MenuOrgOverride> existing = overrideRepository.findByOrgIdAndMenuCodeAndItemIdAndAction(orgId, code, itemId, "hide");
        if (existing.isPresent()) return; // already hidden

        MenuOrgOverride override = new MenuOrgOverride();
        override.setOrgId(orgId);
        override.setMenuCode(code);
        override.setItemId(itemId);
        override.setAction("hide");
        overrideRepository.save(override);
    }

    /**
     * Unhide (restore) a menu item for this org.
     */
    @Transactional
    public void unhideItem(String orgId, String code, UUID itemId) {
        setOrgContext(orgId);
        overrideRepository.deleteByOrgIdAndMenuCodeAndItemId(orgId, code, itemId);
    }

    /**
     * Set app.current_org in the DB session so RLS allows org-specific row access.
     * Must be called within a @Transactional method.
     */
    private void setOrgContext(String orgId) {
        if (orgId != null && !orgId.isBlank() && !"*".equals(orgId)) {
            jdbcTemplate.queryForObject("SELECT set_config('app.current_org', ?, true)", String.class, orgId);
        }
    }

    /**
     * Modify a menu item for this org (label, icon, screenSlug).
     */
    @Transactional
    public void modifyItem(String orgId, String code, UUID itemId, Map<String, String> changes) {
        setOrgContext(orgId);
        Optional<MenuOrgOverride> existing = overrideRepository.findByOrgIdAndMenuCodeAndItemIdAndAction(orgId, code, itemId, "modify");
        MenuOrgOverride override;
        if (existing.isPresent()) {
            override = existing.get();
        } else {
            override = new MenuOrgOverride();
            override.setOrgId(orgId);
            override.setMenuCode(code);
            override.setItemId(itemId);
            override.setAction("modify");
        }
        try {
            // Merge existing changes with new ones
            Map<String, String> merged = new HashMap<>();
            if (override.getData() != null) {
                merged.putAll(objectMapper.readValue(override.getData(), Map.class));
            }
            merged.putAll(changes);
            override.setData(objectMapper.writeValueAsString(merged));
        } catch (Exception e) {
            log.error("Failed to serialize override data", e);
        }
        overrideRepository.save(override);
    }

    /**
     * Add a custom menu item for this org.
     */
    @Transactional
    public MenuOrgOverride addCustomItem(String orgId, String code, Map<String, Object> itemData) {
        setOrgContext(orgId);
        MenuOrgOverride override = new MenuOrgOverride();
        override.setOrgId(orgId);
        override.setMenuCode(code);
        override.setItemId(null); // custom items have no global item reference
        override.setAction("add");
        try {
            override.setData(objectMapper.writeValueAsString(itemData));
        } catch (Exception e) {
            log.error("Failed to serialize custom item data", e);
        }
        return overrideRepository.save(override);
    }

    /**
     * Reorder items for this org.
     */
    @Transactional
    public void reorderForOrg(String orgId, String code, List<Map<String, Object>> ordering) {
        setOrgContext(orgId);
        for (Map<String, Object> entry : ordering) {
            UUID itemId = UUID.fromString((String) entry.get("id"));
            int position = ((Number) entry.get("position")).intValue();

            Optional<MenuOrgOverride> existing = overrideRepository.findByOrgIdAndMenuCodeAndItemIdAndAction(orgId, code, itemId, "reorder");
            MenuOrgOverride override;
            if (existing.isPresent()) {
                override = existing.get();
            } else {
                override = new MenuOrgOverride();
                override.setOrgId(orgId);
                override.setMenuCode(code);
                override.setItemId(itemId);
                override.setAction("reorder");
            }
            try {
                override.setData(objectMapper.writeValueAsString(Map.of("position", position)));
            } catch (Exception e) {
                log.error("Failed to serialize reorder data", e);
            }
            overrideRepository.save(override);
        }
    }

    /**
     * Delete all org overrides (revert to global default).
     */
    @Transactional
    public void deleteOrgOverrides(String orgId, String code) {
        setOrgContext(orgId);
        overrideRepository.deleteByOrgIdAndMenuCode(orgId, code);
    }

    /**
     * Delete a single org override by its ID.
     */
    @Transactional
    public void deleteOverride(UUID overrideId) {
        overrideRepository.deleteById(overrideId);
    }

    /**
     * Get all overrides for an org menu (for the UI to show hidden items etc.)
     */
    public List<MenuOrgOverride> getOrgOverrides(String orgId, String code) {
        return overrideRepository.findByOrgIdAndMenuCode(orgId, code);
    }

    /**
     * Reset menu to defaults. Delete org overrides + any legacy cloned menus.
     * If operating on global, re-seed from migration defaults.
     */
    @Transactional
    public MenuDetailDto resetToDefaults(String orgId, String code) {
        // Delete all org overrides
        if (!"*".equals(orgId)) {
            overrideRepository.deleteByOrgIdAndMenuCode(orgId, code);
        }

        // If operating on global default, re-seed
        if ("*".equals(orgId)) {
            Optional<Menu> globalMenu = menuRepository.findByCodeAndOrgIdAndPracticeTypeCode(code, "*", "*");
            if (globalMenu.isPresent()) {
                menuItemRepository.deleteByMenuId(globalMenu.get().getId());
                menuRepository.delete(globalMenu.get());
            }
            if ("ehr-sidebar".equals(code)) {
                reseedEhrSidebar();
            }
        }

        return getMenuByCode(orgId, code);
    }

    private void reseedEhrSidebar() {
        String menuId = "a0000000-0000-0000-0000-000000000001";

        jdbcTemplate.update(
            "INSERT INTO menu (id, code, name, location, status, org_id) VALUES (?::uuid, ?, ?, ?, ?, ?) ON CONFLICT (code, org_id) DO NOTHING",
            menuId, "ehr-sidebar", "EHR Sidebar Navigation", "SIDEBAR", "PUBLISHED", "*"
        );

        // Top-level items
        String[][] topItems = {
            {"b0000000-0000-0000-0000-000000000001", "calendar",    "Calendar",     "Calendar",      "/calendar",     "0"},
            {"b0000000-0000-0000-0000-000000000002", "appointments","Appointments",  "CalendarCheck", "/appointments", "1"},
            {"b0000000-0000-0000-0000-000000000003", "patients",    "Patients",      "Users",         null,            "2"},
            {"b0000000-0000-0000-0000-000000000004", "inventory",   "Inventory",     "Package",       null,            "3"},
            {"b0000000-0000-0000-0000-000000000005", "recall",      "Recall",        "Bell",          "/recall",       "4"},
            {"b0000000-0000-0000-0000-000000000006", "reports",     "Reports",       "BarChart3",     null,            "5"},
            {"b0000000-0000-0000-0000-000000000007", "settings",    "Settings",      "Settings",      null,            "6"},
            {"b0000000-0000-0000-0000-000000000008", "labs",        "Labs",          "FlaskConical",  null,            "7"},
        };

        for (String[] item : topItems) {
            insertMenuItem(item[0], menuId, null, item[1], item[2], item[3], item[4], Integer.parseInt(item[5]));
        }

        // Patients sub-items
        String patientsId = "b0000000-0000-0000-0000-000000000003";
        String[][] patientsSub = {
            {"c0000000-0000-0000-0000-000000000001", "patient-list",     "Patient List",     "List",          "/patients",                  "0"},
            {"c0000000-0000-0000-0000-000000000002", "encounters",       "Encounters",       "ClipboardList", "/all-encounters",            "1"},
            {"c0000000-0000-0000-0000-000000000003", "messaging",        "Messaging",        "MessageSquare", "/messaging",                 "2"},
            {"c0000000-0000-0000-0000-000000000004", "education",        "Education",        "GraduationCap", "/patient_education",         "3"},
            {"c0000000-0000-0000-0000-000000000005", "codes-list",       "Codes List",       "FileCode",      "/patients/codes",            "4"},
            {"c0000000-0000-0000-0000-000000000006", "claim-management", "Claim Management", "Receipt",       "/patients/claim-management", "5"},
        };
        for (String[] item : patientsSub) {
            insertMenuItem(item[0], menuId, patientsId, item[1], item[2], item[3], item[4], Integer.parseInt(item[5]));
        }

        // Inventory sub-items
        String invId = "b0000000-0000-0000-0000-000000000004";
        String[][] invSub = {
            {"c0000000-0000-0000-0000-000000000010", "inv-dashboard",   "Dashboard",   "LayoutDashboard", "/inventory-management",             "0"},
            {"c0000000-0000-0000-0000-000000000011", "inv-inventory",   "Inventory",   "Package",         "/inventory-management/inventory",   "1"},
            {"c0000000-0000-0000-0000-000000000012", "inv-orders",      "Orders",      "ShoppingCart",    "/inventory-management/orders",      "2"},
            {"c0000000-0000-0000-0000-000000000013", "inv-records",     "Records",     "FileText",        "/inventory-management/records",     "3"},
            {"c0000000-0000-0000-0000-000000000014", "inv-suppliers",   "Suppliers",   "Truck",           "/inventory-management/suppliers",   "4"},
            {"c0000000-0000-0000-0000-000000000015", "inv-maintenance", "Maintenance", "Wrench",          "/inventory-management/maintenance", "5"},
            {"c0000000-0000-0000-0000-000000000016", "inv-settings",    "Settings",    "Settings",        "/inventory-management/settings",    "6"},
        };
        for (String[] item : invSub) {
            insertMenuItem(item[0], menuId, invId, item[1], item[2], item[3], item[4], Integer.parseInt(item[5]));
        }

        // Reports sub-items
        String reportsId = "b0000000-0000-0000-0000-000000000006";
        String[][] reportsSub = {
            {"c0000000-0000-0000-0000-000000000020", "report-patient",     "Patient Report",     "Users",         "/reports/patient",     "0"},
            {"c0000000-0000-0000-0000-000000000021", "report-appointment", "Appointment Report", "CalendarCheck", "/reports/appointment", "1"},
            {"c0000000-0000-0000-0000-000000000022", "report-encounter",   "Encounter Report",   "ClipboardList", "/reports/encounter",   "2"},
            {"c0000000-0000-0000-0000-000000000023", "report-payment",     "Payment Reports",    "DollarSign",    "/reports/payment",     "3"},
        };
        for (String[] item : reportsSub) {
            insertMenuItem(item[0], menuId, reportsId, item[1], item[2], item[3], item[4], Integer.parseInt(item[5]));
        }

        // Settings sub-items
        String settingsId = "b0000000-0000-0000-0000-000000000007";
        String[][] settingsSub = {
            {"c0000000-0000-0000-0000-000000000030", "settings-providers",          "Providers",          "UserCog",      "/settings/providers",          "0"},
            {"c0000000-0000-0000-0000-000000000031", "settings-referral-providers",  "Referral Providers",  "UserPlus",    "/settings/referral-providers",  "1"},
            {"c0000000-0000-0000-0000-000000000032", "settings-referral-practices",  "Referral Practices",  "Building",    "/settings/referral-practices",  "2"},
            {"c0000000-0000-0000-0000-000000000033", "settings-insurance",           "Insurance Companies", "Shield",      "/settings/insurance",           "3"},
            {"c0000000-0000-0000-0000-000000000034", "settings-documents",           "Documents",           "FileText",    "/settings/Documents",           "4"},
            {"c0000000-0000-0000-0000-000000000035", "settings-template-documents",  "Template Documents",  "FilePlus",    "/settings/templateDocument",    "5"},
            {"c0000000-0000-0000-0000-000000000036", "settings-codes",               "Codes",               "FileCode",    "/settings/codes",               "6"},
            {"c0000000-0000-0000-0000-000000000037", "settings-integration",         "Integration",         "Plug",        "/settings/config",              "7"},
            {"c0000000-0000-0000-0000-000000000038", "settings-services",            "Services",            "Briefcase",   "/settings/services",            "8"},
            {"c0000000-0000-0000-0000-000000000039", "settings-billing",             "Billing",             "CreditCard",  "/settings/billing",             "9"},
            {"c0000000-0000-0000-0000-000000000040", "settings-forms",               "Forms",               "FileInput",   null,                            "10"},
            {"c0000000-0000-0000-0000-000000000041", "settings-facilities",          "Facilities",          "Building2",   "/settings/facilities",          "11"},
            {"c0000000-0000-0000-0000-000000000042", "settings-practice",            "Practice",            "Stethoscope", "/settings/practice",            "12"},
            {"c0000000-0000-0000-0000-000000000060", "settings-layout",              "Layout Settings",     "LayoutDashboard", null,                        "13"},
            // Menu Configuration moved under Layout Settings
        };
        for (String[] item : settingsSub) {
            insertMenuItem(item[0], menuId, settingsId, item[1], item[2], item[3], item[4], Integer.parseInt(item[5]));
        }

        // Labs sub-items
        String labsId = "b0000000-0000-0000-0000-000000000008";
        insertMenuItem("c0000000-0000-0000-0000-000000000050", menuId, labsId, "labs-orders",  "Lab Orders",  "TestTube",     "/labs/orders",  0);
        insertMenuItem("c0000000-0000-0000-0000-000000000051", menuId, labsId, "labs-results", "Lab Results", "FileBarChart", "/labs/results", 1);

        // Settings > Layout Settings sub-items
        String layoutId = "c0000000-0000-0000-0000-000000000060";
        insertMenuItem("c0000000-0000-0000-0000-000000000043", menuId, layoutId, "settings-chart", "Chart", "LayoutGrid", "/settings/layout-settings", 0);
        insertMenuItem("c0000000-0000-0000-0000-000000000044", menuId, layoutId, "settings-menu", "Menu", "Menu", "/settings/menu-configuration", 1);
        insertMenuItem("c0000000-0000-0000-0000-000000000045", menuId, layoutId, "settings-encounter", "Encounter", "ClipboardList", "/settings/encounter-settings", 2);

        // Settings > Forms sub-items (3rd level)
        String formsId = "c0000000-0000-0000-0000-000000000040";
        insertMenuItem("d0000000-0000-0000-0000-000000000001", menuId, formsId, "settings-forms-lists",      "Lists",              "List",          "/settings/forms/lists", 0);
        insertMenuItem("d0000000-0000-0000-0000-000000000002", menuId, formsId, "settings-forms-encounters", "Encounter Sections", "ClipboardList", "/settings/forms/admin", 1);

    }

    private void insertMenuItem(String id, String menuId, String parentId, String itemKey, String label, String icon, String screenSlug, int position) {
        jdbcTemplate.update(
            "INSERT INTO menu_item (id, menu_id, parent_id, item_key, label, icon, screen_slug, position) VALUES (?::uuid, ?::uuid, ?::uuid, ?, ?, ?, ?, ?)",
            id, menuId, parentId, itemKey, label, icon, screenSlug, position
        );
    }

    /**
     * Update a single menu item.
     */
    @Transactional
    public MenuItem updateItem(UUID itemId, String label, String icon, String screenSlug) {
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Menu item not found: " + itemId));
        if (label != null) item.setLabel(label);
        if (icon != null) item.setIcon(icon);
        if (screenSlug != null) item.setScreenSlug(screenSlug);
        return menuItemRepository.save(item);
    }

    /**
     * Add a new menu item.
     */
    @Transactional
    public MenuItem addItem(UUID menuId, MenuItem item) {
        item.setMenuId(menuId);
        return menuItemRepository.save(item);
    }

    /**
     * Delete a menu item and its children.
     */
    @Transactional
    public void deleteItem(UUID itemId) {
        menuItemRepository.deleteById(itemId);
    }

    /**
     * Reorder items.
     */
    @Transactional
    public void reorderItems(List<Map<String, Object>> ordering) {
        for (Map<String, Object> entry : ordering) {
            UUID id = UUID.fromString((String) entry.get("id"));
            int position = ((Number) entry.get("position")).intValue();
            menuItemRepository.findById(id).ifPresent(item -> {
                item.setPosition(position);
                menuItemRepository.save(item);
            });
        }
    }

    // ──────────────────────────────────────────
    // Private: apply org overrides to global menu
    // ──────────────────────────────────────────

    private MenuDetailDto applyOverrides(MenuDetailDto detail, List<MenuOrgOverride> overrides) {
        // Collect hidden item IDs
        Set<String> hiddenIds = overrides.stream()
                .filter(o -> "hide".equals(o.getAction()) && o.getItemId() != null)
                .map(o -> o.getItemId().toString())
                .collect(Collectors.toSet());

        // Collect modifications
        Map<String, Map<String, String>> modifications = new HashMap<>();
        for (MenuOrgOverride o : overrides) {
            if ("modify".equals(o.getAction()) && o.getItemId() != null && o.getData() != null) {
                try {
                    Map<String, String> data = objectMapper.readValue(o.getData(), Map.class);
                    modifications.put(o.getItemId().toString(), data);
                } catch (Exception ignored) {}
            }
        }

        // Collect reorders
        Map<String, Integer> reorders = new HashMap<>();
        for (MenuOrgOverride o : overrides) {
            if ("reorder".equals(o.getAction()) && o.getItemId() != null && o.getData() != null) {
                try {
                    Map<String, Object> data = objectMapper.readValue(o.getData(), Map.class);
                    Object pos = data.get("position");
                    if (pos instanceof Number) {
                        reorders.put(o.getItemId().toString(), ((Number) pos).intValue());
                    }
                } catch (Exception ignored) {}
            }
        }

        // Collect custom items to add
        List<MenuOrgOverride> customItems = overrides.stream()
                .filter(o -> "add".equals(o.getAction()))
                .toList();

        // Apply overrides to the tree
        List<MenuDetailDto.MenuItemNode> filtered = applyOverridesToNodes(detail.getItems(), hiddenIds, modifications, reorders);

        // Add custom items
        for (MenuOrgOverride custom : customItems) {
            if (custom.getData() == null) continue;
            try {
                Map<String, Object> data = objectMapper.readValue(custom.getData(), Map.class);
                MenuDetailDto.MenuItemNode node = new MenuDetailDto.MenuItemNode();
                MenuDetailDto.ItemData itemData = new MenuDetailDto.ItemData();
                itemData.setId(custom.getId().toString());
                itemData.setItemKey((String) data.getOrDefault("itemKey", "custom-" + custom.getId()));
                itemData.setLabel((String) data.getOrDefault("label", "Custom Item"));
                itemData.setIcon((String) data.get("icon"));
                itemData.setScreenSlug((String) data.get("screenSlug"));
                itemData.setPosition(data.containsKey("position") ? ((Number) data.get("position")).intValue() : 99);
                node.setItem(itemData);
                node.setChildren(new ArrayList<>());

                // Add to root or to a parent
                String parentId = (String) data.get("parentId");
                if (parentId != null) {
                    addToParent(filtered, parentId, node);
                } else {
                    filtered.add(node);
                }
            } catch (Exception ignored) {}
        }

        // Sort by position
        filtered.sort(Comparator.comparingInt(n -> n.getItem().getPosition()));

        detail.setItems(filtered);
        return detail;
    }

    private List<MenuDetailDto.MenuItemNode> applyOverridesToNodes(
            List<MenuDetailDto.MenuItemNode> nodes,
            Set<String> hiddenIds,
            Map<String, Map<String, String>> modifications,
            Map<String, Integer> reorders) {
        List<MenuDetailDto.MenuItemNode> result = new ArrayList<>();
        for (MenuDetailDto.MenuItemNode node : nodes) {
            String id = node.getItem().getId();

            // Skip hidden items (and their children)
            if (hiddenIds.contains(id)) continue;

            // Apply modifications
            Map<String, String> mods = modifications.get(id);
            if (mods != null) {
                if (mods.containsKey("label")) node.getItem().setLabel(mods.get("label"));
                if (mods.containsKey("icon")) node.getItem().setIcon(mods.get("icon"));
                if (mods.containsKey("screenSlug")) node.getItem().setScreenSlug(mods.get("screenSlug"));
            }

            // Apply reorder
            Integer newPos = reorders.get(id);
            if (newPos != null) node.getItem().setPosition(newPos);

            // Recurse into children
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                node.setChildren(applyOverridesToNodes(node.getChildren(), hiddenIds, modifications, reorders));
            }

            result.add(node);
        }
        result.sort(Comparator.comparingInt(n -> n.getItem().getPosition()));
        return result;
    }

    private void addToParent(List<MenuDetailDto.MenuItemNode> nodes, String parentId, MenuDetailDto.MenuItemNode newNode) {
        for (MenuDetailDto.MenuItemNode node : nodes) {
            if (node.getItem().getId().equals(parentId)) {
                if (node.getChildren() == null) node.setChildren(new ArrayList<>());
                node.getChildren().add(newNode);
                return;
            }
            if (node.getChildren() != null) {
                addToParent(node.getChildren(), parentId, newNode);
            }
        }
    }

    // ──────────────────────────────────────────
    // Private: build tree structure for response
    // ──────────────────────────────────────────

    private MenuDetailDto buildMenuDetail(Menu menu) {
        List<MenuItem> allItems = menuItemRepository.findByMenuIdOrderByPosition(menu.getId());

        // Group by parent
        Map<UUID, List<MenuItem>> byParent = allItems.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getParentId() != null ? i.getParentId() : UUID.fromString("00000000-0000-0000-0000-000000000000"),
                        Collectors.toList()));

        // Build tree from roots (parent = null)
        List<MenuDetailDto.MenuItemNode> rootNodes = buildChildren(
                UUID.fromString("00000000-0000-0000-0000-000000000000"), byParent);

        MenuDetailDto dto = new MenuDetailDto();
        MenuDetailDto.MenuInfo info = new MenuDetailDto.MenuInfo();
        info.setId(menu.getId().toString());
        info.setCode(menu.getCode());
        info.setName(menu.getName());
        info.setOrgId(menu.getOrgId());
        dto.setMenu(info);
        dto.setItems(rootNodes);
        return dto;
    }

    private List<MenuDetailDto.MenuItemNode> buildChildren(UUID parentKey, Map<UUID, List<MenuItem>> byParent) {
        List<MenuItem> children = byParent.getOrDefault(parentKey, List.of());
        return children.stream().map(item -> {
            MenuDetailDto.MenuItemNode node = new MenuDetailDto.MenuItemNode();
            MenuDetailDto.ItemData data = new MenuDetailDto.ItemData();
            data.setId(item.getId().toString());
            data.setItemKey(item.getItemKey());
            data.setLabel(item.getLabel());
            data.setIcon(item.getIcon());
            data.setScreenSlug(item.getScreenSlug());
            data.setPosition(item.getPosition());
            data.setRoles(item.getRoles());
            data.setRequiredPermission(item.getRequiredPermission());
            node.setItem(data);
            node.setChildren(buildChildren(item.getId(), byParent));
            return node;
        }).collect(Collectors.toList());
    }
}
