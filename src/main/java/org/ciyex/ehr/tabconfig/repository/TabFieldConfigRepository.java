package org.ciyex.ehr.tabconfig.repository;

import org.ciyex.ehr.tabconfig.entity.TabFieldConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TabFieldConfigRepository extends JpaRepository<TabFieldConfig, UUID> {

    Optional<TabFieldConfig> findByTabKeyAndPracticeTypeCodeAndOrgId(String tabKey, String practiceTypeCode, String orgId);

    @Query("SELECT t FROM TabFieldConfig t WHERE t.tabKey = :tabKey AND t.practiceTypeCode = :practiceTypeCode AND (t.orgId = :orgId OR t.orgId = '*') ORDER BY CASE WHEN t.orgId = '*' THEN 1 ELSE 0 END")
    List<TabFieldConfig> findByTabKeyAndPracticeTypeForOrg(String tabKey, String practiceTypeCode, String orgId);

    @Query("SELECT t FROM TabFieldConfig t WHERE t.tabKey = :tabKey AND (t.practiceTypeCode = :practiceTypeCode OR t.practiceTypeCode = '*') AND (t.orgId = :orgId OR t.orgId = '*') ORDER BY CASE WHEN t.orgId = '*' THEN 1 ELSE 0 END, CASE WHEN t.practiceTypeCode = '*' THEN 1 ELSE 0 END")
    List<TabFieldConfig> findEffective(String tabKey, String practiceTypeCode, String orgId);

    @Query("SELECT t FROM TabFieldConfig t WHERE (t.practiceTypeCode = :practiceTypeCode OR t.practiceTypeCode = '*') AND (t.orgId = :orgId OR t.orgId = '*') ORDER BY t.position")
    List<TabFieldConfig> findAllForPracticeTypeAndOrg(String practiceTypeCode, String orgId);

    @Query("SELECT DISTINCT t.tabKey FROM TabFieldConfig t WHERE t.practiceTypeCode = '*' AND t.orgId = '*' ORDER BY t.tabKey")
    List<String> findAllUniversalTabKeys();

    List<TabFieldConfig> findByOrgId(String orgId);

    void deleteByTabKeyAndOrgId(String tabKey, String orgId);

    /**
     * Fetch all tab_field_config rows for layout (org + universal fallback).
     * Ordered by category then position for grouping.
     */
    @Query("SELECT t FROM TabFieldConfig t WHERE (t.orgId = :orgId OR t.orgId = '*') AND (t.practiceTypeCode = :ptCode OR t.practiceTypeCode = '*') ORDER BY t.categoryPosition, t.position")
    List<TabFieldConfig> findAllForLayout(String orgId, String ptCode);

    /**
     * Delete all org-specific layout overrides (rows where orgId != '*').
     */
    void deleteByOrgIdAndPracticeTypeCode(String orgId, String practiceTypeCode);
}
