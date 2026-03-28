package org.ciyex.ehr.inventory.repository;

import org.ciyex.ehr.inventory.entity.InventoryCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryCategoryRepository extends JpaRepository<InventoryCategory, Long> {

    List<InventoryCategory> findByOrgAlias(String orgAlias);

    List<InventoryCategory> findByOrgAliasAndParentIsNull(String orgAlias);

    List<InventoryCategory> findByOrgAliasOrOrgAlias(String orgAlias1, String orgAlias2);
}
