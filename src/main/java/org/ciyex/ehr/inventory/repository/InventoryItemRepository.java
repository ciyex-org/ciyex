package org.ciyex.ehr.inventory.repository;

import org.ciyex.ehr.inventory.entity.InventoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    Page<InventoryItem> findByOrgAlias(String orgAlias, Pageable pageable);

    List<InventoryItem> findByOrgAlias(String orgAlias);

    Page<InventoryItem> findByOrgAliasAndStatus(String orgAlias, String status, Pageable pageable);

    long countByOrgAlias(String orgAlias);

    long countByOrgAliasAndStockOnHandLessThanEqual(String orgAlias, Integer stock);

    @Query("SELECT i FROM InventoryItem i WHERE i.orgAlias = :orgAlias AND i.stockOnHand <= i.minStock")
    List<InventoryItem> findLowStockItems(@Param("orgAlias") String orgAlias);

    @Query("SELECT i FROM InventoryItem i WHERE i.orgAlias = :orgAlias " +
           "AND (LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(i.sku) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<InventoryItem> searchByName(@Param("orgAlias") String orgAlias,
                                     @Param("query") String query,
                                     Pageable pageable);

    @Query("SELECT DISTINCT l.item FROM InventoryLot l " +
           "WHERE l.orgAlias = :orgAlias AND l.expiryDate IS NOT NULL AND l.expiryDate < :before")
    List<InventoryItem> findExpiringItems(@Param("orgAlias") String orgAlias,
                                          @Param("before") LocalDate before);
}
