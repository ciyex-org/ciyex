package com.qiaben.ciyex.repository;

import com.azure.core.http.HttpHeaders;
import com.qiaben.ciyex.entity.GpsBillingCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GpsBillingCardRepository extends JpaRepository<GpsBillingCard, Long> {
    HttpHeaders findByUserId(Long userId);
}
