package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.ProviderSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProviderScheduleRepository extends JpaRepository<ProviderSchedule, Long> {

    List<ProviderSchedule> findAllByOrgId(Long orgId);      // like ProviderRepository :contentReference[oaicite:2]{index=2}L13-L16
    List<ProviderSchedule> findByOrgIdAndProviderId(Long orgId, Long providerId);

    @Query("SELECT COUNT(s) FROM ProviderSchedule s WHERE s.orgId = :orgId")
    long countByOrgId(Long orgId);                          // mirrors Provider count :contentReference[oaicite:3]{index=3}L22-L24
}
