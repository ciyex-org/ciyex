package org.ciyex.ehr.usermgmt.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.security.SmartScopeResolver;
import org.ciyex.ehr.usermgmt.dto.RolePermissionDto;
import org.ciyex.ehr.usermgmt.entity.RolePermissionConfig;
import org.ciyex.ehr.usermgmt.repository.RolePermissionConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RolePermissionService {

    /** Role names that cannot be created by org admins — reserved for platform use. */
    private static final Set<String> RESERVED_ROLE_NAMES = Set.of(
            "CIYEX_SUPER_ADMIN", "SUPER_ADMIN"
    );

    private final RolePermissionConfigRepository repo;
    private final PermissionResolver permissionResolver;
    private final SmartScopeResolver smartScopeResolver;

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    @Transactional
    public List<RolePermissionDto> listRoles() {
        String org = orgAlias();
        var roles = repo.findByOrgAliasOrderByRoleNameAsc(org);

        // If org has no roles yet, copy from __SYSTEM__ templates
        if (roles.isEmpty()) {
            var systemRoles = repo.findByOrgAliasOrderByRoleNameAsc("__SYSTEM__");
            if (!systemRoles.isEmpty()) {
                for (var sys : systemRoles) {
                    var copy = RolePermissionConfig.builder()
                            .roleName(sys.getRoleName())
                            .roleLabel(sys.getRoleLabel())
                            .description(sys.getDescription())
                            .permissions(sys.getPermissions() != null ? List.copyOf(sys.getPermissions()) : List.of())
                            .smartScopes(sys.getSmartScopes() != null ? List.copyOf(sys.getSmartScopes()) : List.of())
                            .isSystem(sys.getIsSystem())
                            .isActive(true)
                            .orgAlias(org)
                            .displayOrder(sys.getDisplayOrder())
                            .build();
                    repo.save(copy);
                }
                roles = repo.findByOrgAliasOrderByRoleNameAsc(org);
            }
        }

        return roles.stream()
                .filter(r -> !RESERVED_ROLE_NAMES.contains(r.getRoleName()))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public RolePermissionDto getRole(Long id) {
        return repo.findByIdAndOrgAlias(id, orgAlias())
                .map(this::toDto)
                .orElseThrow(() -> new NoSuchElementException("Role not found: " + id));
    }

    @Transactional
    public RolePermissionDto createRole(RolePermissionDto dto) {
        String org = orgAlias();
        String normalizedName = dto.getRoleName().toUpperCase().replaceAll("[^A-Z0-9_]", "_");

        // Block reserved platform role names
        if (RESERVED_ROLE_NAMES.contains(normalizedName)) {
            throw new IllegalArgumentException("Role name '" + dto.getRoleName() + "' is reserved and cannot be created");
        }

        // Check for duplicate role name
        if (repo.findByOrgAliasAndRoleName(org, dto.getRoleName()).isPresent()) {
            throw new IllegalArgumentException("Role already exists: " + dto.getRoleName());
        }

        var entity = RolePermissionConfig.builder()
                .roleName(normalizedName)
                .roleLabel(dto.getRoleLabel())
                .description(dto.getDescription())
                .permissions(dto.getPermissions() != null ? dto.getPermissions() : List.of())
                .smartScopes(dto.getSmartScopes() != null ? dto.getSmartScopes() : List.of())
                .isSystem(false)
                .isActive(true)
                .orgAlias(org)
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 100)
                .build();

        return toDto(repo.save(entity));
    }

    @Transactional
    public RolePermissionDto updateRole(Long id, RolePermissionDto dto) {
        var entity = repo.findByIdAndOrgAlias(id, orgAlias())
                .orElseThrow(() -> new NoSuchElementException("Role not found: " + id));

        if (dto.getRoleLabel() != null) entity.setRoleLabel(dto.getRoleLabel());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        if (dto.getPermissions() != null) {
            if ("ADMIN".equals(entity.getRoleName()) && dto.getPermissions().isEmpty()) {
                throw new IllegalArgumentException(
                        "Cannot remove all permissions from ADMIN role — this would lock out the organization");
            }
            entity.setPermissions(dto.getPermissions());
        }
        if (dto.getSmartScopes() != null) {
            // Prevent ADMIN from losing all FHIR scopes (locks out the org)
            if ("ADMIN".equals(entity.getRoleName()) && dto.getSmartScopes().isEmpty()) {
                throw new IllegalArgumentException(
                        "Cannot remove all FHIR scopes from ADMIN role — this would lock out the organization");
            }
            entity.setSmartScopes(dto.getSmartScopes());
        }
        if (dto.getIsActive() != null) entity.setIsActive(dto.getIsActive());
        if (dto.getDisplayOrder() != null) entity.setDisplayOrder(dto.getDisplayOrder());

        var saved = toDto(repo.save(entity));
        permissionResolver.evictCache(orgAlias());
        smartScopeResolver.evictCache(orgAlias());
        return saved;
    }

    @Transactional
    public void deleteRole(Long id) {
        var entity = repo.findByIdAndOrgAlias(id, orgAlias())
                .orElseThrow(() -> new NoSuchElementException("Role not found: " + id));

        if (Boolean.TRUE.equals(entity.getIsSystem())) {
            throw new IllegalStateException("System roles cannot be deleted");
        }

        repo.delete(entity);
        permissionResolver.evictCache(orgAlias());
        smartScopeResolver.evictCache(orgAlias());
    }

    private RolePermissionDto toDto(RolePermissionConfig e) {
        return RolePermissionDto.builder()
                .id(e.getId())
                .roleName(e.getRoleName())
                .roleLabel(e.getRoleLabel())
                .description(e.getDescription())
                .permissions(e.getPermissions())
                .smartScopes(e.getSmartScopes())
                .isSystem(e.getIsSystem())
                .isActive(e.getIsActive())
                .displayOrder(e.getDisplayOrder())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }
}
