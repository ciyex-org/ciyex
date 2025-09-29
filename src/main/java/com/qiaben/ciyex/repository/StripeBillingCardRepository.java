package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.StripeBillingCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StripeBillingCardRepository extends JpaRepository<StripeBillingCard, Long> {

    List<StripeBillingCard> findByUserIdAndOrgId(Long userId, Long orgId);

    List<StripeBillingCard> findByOrgId(Long orgId);

    Optional<StripeBillingCard> findByIdAndOrgId(Long id, Long orgId);

    void deleteByIdAndUserIdAndOrgId(Long id, Long userId, Long orgId);

    // 🔥 Add this method for BillingHistoryService
    Optional<StripeBillingCard> findFirstByUserIdAndOrgIdAndIsDefaultTrue(Long userId, Long orgId);
}
