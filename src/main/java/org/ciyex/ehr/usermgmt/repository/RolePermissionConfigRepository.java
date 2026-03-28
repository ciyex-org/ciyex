package org.ciyex.ehr.usermgmt.repository;

import org.ciyex.ehr.usermgmt.entity.RolePermissionConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolePermissionConfigRepository extends JpaRepository<RolePermissionConfig, Long> {

    List<RolePermissionConfig> findByOrgAliasAndIsActiveTrueOrderByRoleNameAsc(String orgAlias);

    List<RolePermissionConfig> findByOrgAliasOrderByRoleNameAsc(String orgAlias);

    Optional<RolePermissionConfig> findByOrgAliasAndRoleName(String orgAlias, String roleName);

    Optional<RolePermissionConfig> findByIdAndOrgAlias(Long id, String orgAlias);
}
