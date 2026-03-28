package org.ciyex.ehr.portal.repository;

import org.ciyex.ehr.portal.entity.PortalConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PortalConfigRepository extends JpaRepository<PortalConfig, Long> {
    Optional<PortalConfig> findByOrgAlias(String orgAlias);
}
