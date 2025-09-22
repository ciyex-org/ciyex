package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findByOrgId(Long orgId);
    Page<Supplier> findAllByOrgId(Long orgId, Pageable pageable);
    Long countByOrgId(Long orgId);
}
