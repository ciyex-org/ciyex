package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.GpsPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GpsPaymentRepository extends JpaRepository<GpsPayment, Long> {
    List<GpsPayment> findByOrgId(Long orgId);
    List<GpsPayment> findByOrgIdAndUserId(Long orgId, Long userId);
}
