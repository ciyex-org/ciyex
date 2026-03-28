package org.ciyex.ehr.menu.repository;

import org.ciyex.ehr.menu.entity.MenuOrgOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MenuOrgOverrideRepository extends JpaRepository<MenuOrgOverride, UUID> {

    List<MenuOrgOverride> findByOrgIdAndMenuCode(String orgId, String menuCode);

    Optional<MenuOrgOverride> findByOrgIdAndMenuCodeAndItemId(String orgId, String menuCode, UUID itemId);

    Optional<MenuOrgOverride> findByOrgIdAndMenuCodeAndItemIdAndAction(String orgId, String menuCode, UUID itemId, String action);

    void deleteByOrgIdAndMenuCode(String orgId, String menuCode);

    void deleteByOrgIdAndMenuCodeAndItemId(String orgId, String menuCode, UUID itemId);

    boolean existsByOrgIdAndMenuCode(String orgId, String menuCode);
}
