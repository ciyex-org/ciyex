package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    List<Inventory> findByOrgId(Long orgId);
    Page<Inventory> findAllByOrgId(Long orgId, Pageable pageable);
    long countByOrgId(Long orgId);
}
