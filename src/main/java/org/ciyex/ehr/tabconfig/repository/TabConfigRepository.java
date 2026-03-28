package org.ciyex.ehr.tabconfig.repository;

import org.ciyex.ehr.tabconfig.entity.TabConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TabConfigRepository extends JpaRepository<TabConfig, UUID> {
    Optional<TabConfig> findByOrgId(String orgId);

    void deleteByOrgId(String orgId);
}
