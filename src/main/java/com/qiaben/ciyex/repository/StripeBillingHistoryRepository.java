package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.StripeBillingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StripeBillingHistoryRepository extends JpaRepository<StripeBillingHistory, Long> {

    List<StripeBillingHistory> findByUserIdAndOrgId(Long userId, Long orgId);

    List<StripeBillingHistory> findByOrgId(Long orgId);

    Optional<StripeBillingHistory> findByStripePaymentIntentId(String stripePaymentIntentId);
}
