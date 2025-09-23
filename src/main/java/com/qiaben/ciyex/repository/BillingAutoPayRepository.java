package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.BillingAutoPay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingAutoPayRepository extends JpaRepository<BillingAutoPay, Long> {
    Optional<BillingAutoPay> findByUserIdAndOrgId(Long userId, Long orgId);
}
