package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.StripeBillingCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StripeBillingCardRepository extends JpaRepository<StripeBillingCard, Long> {

    List<StripeBillingCard> findByOrgId(Long orgId);

    List<StripeBillingCard> findByUserIdAndOrgId(Long userId, Long orgId);

    Optional<StripeBillingCard> findByIdAndOrgId(Long id, Long orgId);

    Optional<StripeBillingCard> findFirstByUserIdAndOrgIdAndIsDefaultTrue(Long userId, Long orgId);

    // ✅ Needed for backfilling customer IDs
    List<StripeBillingCard> findByStripeCustomerIdIsNullAndOrgId(Long orgId);
}
