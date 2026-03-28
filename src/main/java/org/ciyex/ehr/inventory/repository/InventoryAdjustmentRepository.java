package org.ciyex.ehr.inventory.repository;

import org.ciyex.ehr.inventory.entity.InventoryAdjustment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, Long> {

    List<InventoryAdjustment> findByItemId(Long itemId);

    Page<InventoryAdjustment> findByOrgAlias(String orgAlias, Pageable pageable);

    List<InventoryAdjustment> findByItemIdOrderByCreatedAtDesc(Long itemId);
}
