package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProviderRepository extends JpaRepository<Provider, Long> {
    @Query("SELECT p FROM Provider p WHERE p.orgId = :orgId")
    List<Provider> findAllByOrgId(@Param("orgId") Long orgId);

    Optional<Provider> findByExternalId(String externalId);

    @Query("SELECT p.externalId FROM Provider p WHERE p.orgId = :orgId")
    List<String> findAllExternalIdsByOrgId(Long orgId);

    @Query("SELECT COUNT(p) FROM Provider p WHERE p.orgId = :orgId")
    long countByOrgId(@Param("orgId") Long orgId);

    @Query("SELECT p FROM Provider p WHERE p.orgId = :orgId AND p.externalId = :externalId")
    Optional<Provider> findByExternalIdAndOrgId(Long orgId, String externalId);
}