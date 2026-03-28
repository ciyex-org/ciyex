package org.ciyex.ehr.usermgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RolePermissionDto {
    private Long id;
    private String roleName;
    private String roleLabel;
    private String description;
    private List<String> permissions;
    /** SMART on FHIR API-level scopes configurable per role per org. */
    private List<String> smartScopes;
    private Boolean isSystem;
    private Boolean isActive;
    private Integer displayOrder;
    private String createdAt;
    private String updatedAt;
}
