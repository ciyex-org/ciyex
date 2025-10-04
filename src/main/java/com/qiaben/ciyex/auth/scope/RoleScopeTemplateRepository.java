package com.qiaben.ciyex.auth.scope;

import com.qiaben.ciyex.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RoleScopeTemplateRepository extends JpaRepository<RoleScopeTemplate, Long> {
    boolean existsByRoleAndScope(RoleName role, Scope scope);

    @Query("select rst.scope from RoleScopeTemplate rst where rst.role = :role")
    List<Scope> findScopesByRole(RoleName role);
}
