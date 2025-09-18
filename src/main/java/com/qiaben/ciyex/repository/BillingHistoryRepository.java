package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.BillingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillingHistoryRepository extends JpaRepository<BillingHistory, Long> {

    List<BillingHistory> findByUserIdAndOrgId(Long userId, Long orgId);

    List<BillingHistory> findByOrgId(Long orgId);

    Optional<BillingHistory> findByStripePaymentIntentId(String stripePaymentIntentId);
}
