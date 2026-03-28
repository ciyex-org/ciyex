package org.ciyex.ehr.inventory.repository;

import org.ciyex.ehr.inventory.entity.InventoryLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InventoryLotRepository extends JpaRepository<InventoryLot, Long> {

    List<InventoryLot> findByItemId(Long itemId);

    List<InventoryLot> findByItemIdOrderByExpiryDateAsc(Long itemId);

    List<InventoryLot> findByOrgAliasAndExpiryDateBefore(String orgAlias, LocalDate date);
}
