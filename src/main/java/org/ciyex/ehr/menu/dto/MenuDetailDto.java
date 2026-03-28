package org.ciyex.ehr.menu.dto;

import lombok.Data;
import java.util.List;

@Data
public class MenuDetailDto {

    private MenuInfo menu;
    private List<MenuItemNode> items;

    @Data
    public static class MenuInfo {
        private String id;
        private String code;
        private String name;
        private String orgId;
    }

    @Data
    public static class MenuItemNode {
        private ItemData item;
        private List<MenuItemNode> children;
    }

    @Data
    public static class ItemData {
        private String id;
        private String itemKey;
        private String label;
        private String icon;
        private String screenSlug;
        private int position;
        private List<String> roles;
        private String requiredPermission;
    }
}
