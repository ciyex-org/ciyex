package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.OrgConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrgConfigRepository extends JpaRepository<OrgConfig, Long> {
    Optional<OrgConfig> findByOrgId(Long orgId);
}

