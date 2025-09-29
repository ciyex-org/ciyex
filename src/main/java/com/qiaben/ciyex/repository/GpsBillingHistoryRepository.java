package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.GpsBillingHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GpsBillingHistoryRepository extends JpaRepository<GpsBillingHistory, Long> {
    List<GpsBillingHistory> findByOrgIdAndUserIdOrderByCreatedAtDesc(Long orgId, Long userId);
    List<GpsBillingHistory> findByOrgIdOrderByCreatedAtDesc(Long orgId);
    Optional<GpsBillingHistory> findByGpsTransactionId(String gpsTransactionId);
}
