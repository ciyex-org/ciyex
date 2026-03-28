package org.ciyex.ehr.marketplace.repository;

import org.ciyex.ehr.marketplace.entity.AppLaunchLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AppLaunchLogRepository extends JpaRepository<AppLaunchLog, UUID> {

    List<AppLaunchLog> findByOrgIdOrderByLaunchedAtDesc(String orgId);

    List<AppLaunchLog> findByOrgIdAndAppSlugOrderByLaunchedAtDesc(String orgId, String appSlug);
}
