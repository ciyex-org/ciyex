package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.GpsBillingCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GpsBillingCardRepository extends JpaRepository<GpsBillingCard, Long> {

    List<GpsBillingCard> findByOrgId(Long orgId);

    List<GpsBillingCard> findByUserIdAndOrgId(Long userId, Long orgId);

    Optional<GpsBillingCard> findByIdAndOrgId(Long id, Long orgId);

    Optional<GpsBillingCard> findFirstByUserIdAndOrgIdAndIsDefaultTrue(Long userId, Long orgId);
}
