package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.dto.MonthlyOrderCountDto;
import com.qiaben.ciyex.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByOrgId(Long orgId);
    Page<Order> findAllByOrgId(Long orgId, Pageable pageable);
    long countByOrgId(Long orgId);

    @Query(
            value = """
            SELECT EXTRACT(MONTH FROM TO_DATE(o.date, 'YYYY-MM-DD')) AS month,
                   COUNT(*) AS count
            FROM orders o
            WHERE o.org_id = :orgId
            GROUP BY EXTRACT(MONTH FROM TO_DATE(o.date, 'YYYY-MM-DD'))
            ORDER BY EXTRACT(MONTH FROM TO_DATE(o.date, 'YYYY-MM-DD'))
            """,
            nativeQuery = true)
    List<Object[]> countOrdersByMonth(@Param("orgId") Long orgId);
    long countByOrgIdAndStatus(Long orgId, String status);



}
