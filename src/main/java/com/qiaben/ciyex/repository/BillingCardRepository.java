package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.BillingCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillingCardRepository extends JpaRepository<BillingCard, Long> {

    /** Single card for a user/org (legacy) */
    Optional<BillingCard> findByUserIdAndOrgId(Long userId, Long orgId);

    /** All cards for a user/org, newest first */
    List<BillingCard> findByUserIdAndOrgIdOrderByCreatedAtDesc(Long userId, Long orgId);

    /** All cards within an org */
    List<BillingCard> findByOrgId(Long orgId);

    /** Oldest card in org (used when reassigning default after delete) */
    Optional<BillingCard> findFirstByOrgIdOrderByCreatedAtAsc(Long orgId);

    /** Default card for a given user/org */
    Optional<BillingCard> findFirstByUserIdAndOrgIdAndIsDefaultTrue(Long userId, Long orgId);
}
