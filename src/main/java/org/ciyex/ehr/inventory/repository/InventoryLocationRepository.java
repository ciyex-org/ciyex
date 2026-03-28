package org.ciyex.ehr.inventory.repository;

import org.ciyex.ehr.inventory.entity.InventoryLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryLocationRepository extends JpaRepository<InventoryLocation, Long> {

    List<InventoryLocation> findByOrgAlias(String orgAlias);

    List<InventoryLocation> findByOrgAliasAndParentIsNull(String orgAlias);

    List<InventoryLocation> findByOrgAliasOrOrgAlias(String orgAlias1, String orgAlias2);
}
