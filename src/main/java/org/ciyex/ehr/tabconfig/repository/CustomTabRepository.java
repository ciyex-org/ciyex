package org.ciyex.ehr.tabconfig.repository;

import org.ciyex.ehr.tabconfig.entity.CustomTab;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CustomTabRepository extends JpaRepository<CustomTab, UUID> {
    List<CustomTab> findByOrgIdOrderByPosition(String orgId);
}
