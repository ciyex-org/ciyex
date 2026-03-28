package org.ciyex.ehr.menu.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.menu.dto.MenuDetailDto;
import org.ciyex.ehr.menu.entity.MenuItem;
import org.ciyex.ehr.menu.entity.MenuOrgOverride;
import org.ciyex.ehr.menu.service.MenuService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@PreAuthorize("hasAuthority('SCOPE_user/Organization.read')")
@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
@Slf4j
public class MenuController {

    private final MenuService menuService;

    /**
     * GET /api/menus/{code}?practiceType=general-dentistry — Resolved menu (practice-type + org overrides applied)
     */
    @GetMapping("/{code}")
    public ResponseEntity<?> getMenu(
            @PathVariable String code,
            @RequestParam(value = "practiceType", defaultValue = "*") String practiceType) {
        String orgId = getOrgId();
        MenuDetailDto detail = menuService.getMenuByCode(orgId, code, practiceType);
        if (detail == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(detail);
    }

    /**
     * GET /api/menus/{code}/has-custom — Check if org has any customizations
     */
    @GetMapping("/{code}/has-custom")
    public ResponseEntity<Map<String, Boolean>> hasCustom(@PathVariable String code) {
        String orgId = getOrgId();
        boolean hasCustom = menuService.hasOrgMenu(orgId, code);
        return ResponseEntity.ok(Map.of("hasCustom", hasCustom));
    }

    /**
     * GET /api/menus/{code}/overrides — Get all org overrides
     */
    @GetMapping("/{code}/overrides")
    public ResponseEntity<List<MenuOrgOverride>> getOverrides(@PathVariable String code) {
        String orgId = getOrgId();
        return ResponseEntity.ok(menuService.getOrgOverrides(orgId, code));
    }

    /**
     * POST /api/menus/{code}/items/{itemId}/hide — Hide a menu item for this org
     */
    @PostMapping("/{code}/items/{itemId}/hide")
    public ResponseEntity<?> hideItem(@PathVariable String code, @PathVariable UUID itemId) {
        try {
            String orgId = getOrgId();
            menuService.hideItem(orgId, code, itemId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to hide menu item: code={}, itemId={}", code, itemId, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/menus/{code}/items/{itemId}/hide — Unhide (restore) a menu item
     */
    @DeleteMapping("/{code}/items/{itemId}/hide")
    public ResponseEntity<?> unhideItem(@PathVariable String code, @PathVariable UUID itemId) {
        try {
            String orgId = getOrgId();
            menuService.unhideItem(orgId, code, itemId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to unhide menu item: code={}, itemId={}", code, itemId, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/menus/{code}/items/{itemId}/modify — Modify a menu item for this org
     */
    @PutMapping("/{code}/items/{itemId}/modify")
    public ResponseEntity<?> modifyItem(
            @PathVariable String code, @PathVariable UUID itemId,
            @RequestBody Map<String, String> changes) {
        try {
            String orgId = getOrgId();
            menuService.modifyItem(orgId, code, itemId, changes);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to modify menu item: code={}, itemId={}", code, itemId, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/menus/{code}/custom-items — Add a custom menu item for this org
     */
    @PostMapping("/{code}/custom-items")
    public ResponseEntity<?> addCustomItem(@PathVariable String code, @RequestBody Map<String, Object> itemData) {
        try {
            String orgId = getOrgId();
            MenuOrgOverride created = menuService.addCustomItem(orgId, code, itemData);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            log.error("Failed to add custom menu item: code={}", code, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/menus/{code}/reorder — Reorder items for this org
     */
    @PutMapping("/{code}/reorder")
    public ResponseEntity<?> reorderItems(
            @PathVariable String code,
            @RequestBody List<Map<String, Object>> ordering) {
        try {
            String orgId = getOrgId();
            menuService.reorderForOrg(orgId, code, ordering);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to reorder menu items: code={}", code, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/menus/{code}/overrides — Delete all org overrides (reset to defaults)
     */
    @DeleteMapping("/{code}/overrides")
    public ResponseEntity<?> deleteOrgOverrides(@PathVariable String code) {
        try {
            String orgId = getOrgId();
            menuService.deleteOrgOverrides(orgId, code);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Failed to delete menu overrides: code={}", code, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/menus/overrides/{id} — Delete a single override
     */
    @DeleteMapping("/overrides/{id}")
    public ResponseEntity<Void> deleteOverride(@PathVariable UUID id) {
        menuService.deleteOverride(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/menus/{code}/reset — Reset menu to defaults (clear overrides + re-seed global if needed)
     */
    @PostMapping("/{code}/reset")
    public ResponseEntity<?> resetToDefaults(@PathVariable String code) {
        try {
            String orgId = getOrgId();
            MenuDetailDto result = menuService.resetToDefaults(orgId, code);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to reset menu: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ---- Legacy endpoints (kept for backward compatibility) ----

    /**
     * PUT /api/menus/items/{id} — Update a global menu item directly
     */
    @PutMapping("/items/{id}")
    public ResponseEntity<?> updateItem(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        MenuItem updated = menuService.updateItem(id, body.get("label"), body.get("icon"), body.get("screenSlug"));
        return ResponseEntity.ok(updated);
    }

    /**
     * POST /api/menus/{menuId}/items — Add a new global menu item
     */
    @PostMapping("/{menuId}/items")
    public ResponseEntity<?> addItem(@PathVariable UUID menuId, @RequestBody MenuItem item) {
        MenuItem created = menuService.addItem(menuId, item);
        return ResponseEntity.ok(created);
    }

    /**
     * DELETE /api/menus/items/{id} — Delete a global menu item
     */
    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable UUID id) {
        menuService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/menus/{menuId}/items/reorder — Reorder global items
     */
    @PutMapping("/{menuId}/items/reorder")
    public ResponseEntity<Void> reorderGlobalItems(
            @PathVariable UUID menuId,
            @RequestBody List<Map<String, Object>> ordering) {
        menuService.reorderItems(ordering);
        return ResponseEntity.ok().build();
    }

    private String getOrgId() {
        RequestContext ctx = RequestContext.get();
        return ctx != null && ctx.getOrgName() != null ? ctx.getOrgName() : "*";
    }
}
