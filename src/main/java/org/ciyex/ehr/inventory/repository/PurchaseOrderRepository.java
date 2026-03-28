package org.ciyex.ehr.inventory.repository;

import org.ciyex.ehr.inventory.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    Page<PurchaseOrder> findByOrgAlias(String orgAlias, Pageable pageable);

    List<PurchaseOrder> findByOrgAliasAndStatus(String orgAlias, String status);

    long countByOrgAliasAndStatus(String orgAlias, String status);

    long countByOrgAlias(String orgAlias);
}
