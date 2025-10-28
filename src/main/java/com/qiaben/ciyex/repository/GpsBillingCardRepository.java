package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.GpsBillingCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GpsBillingCardRepository extends JpaRepository<GpsBillingCard, Long> {
    List<GpsBillingCard> findByUserId(UUID userId);
}
