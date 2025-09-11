package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByOrgId(Long orgId);
    Page<Order> findAllByOrgId(Long orgId, Pageable pageable);
    long countByOrgId(Long orgId);
}
