package org.ciyex.ehr.marketplace.repository;

import org.ciyex.ehr.marketplace.entity.AppInstallation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppInstallationRepository extends JpaRepository<AppInstallation, UUID> {

    List<AppInstallation> findByOrgIdAndStatus(String orgId, String status);

    List<AppInstallation> findByOrgIdAndStatusNot(String orgId, String status);

    Optional<AppInstallation> findByOrgIdAndAppSlug(String orgId, String appSlug);

    boolean existsByOrgIdAndAppSlugAndStatus(String orgId, String appSlug, String status);

    Optional<AppInstallation> findFirstByAppSlugAndStatus(String appSlug, String status);

    /**
     * Find active installations whose extension_points JSONB array contains
     * any value matching the given prefix pattern (e.g., "patient-chart:%").
     * Uses the JSONB containment operator with a like-style check.
     */
    @Query(value = """
        SELECT ai.* FROM app_installations ai,
               jsonb_array_elements_text(ai.extension_points) AS ep
        WHERE ai.org_id = :orgId
          AND ai.status = 'active'
          AND ep LIKE :contextPrefix
        GROUP BY ai.id
        """, nativeQuery = true)
    List<AppInstallation> findByOrgIdAndExtensionPointPrefix(
            @Param("orgId") String orgId,
            @Param("contextPrefix") String contextPrefix);

    /**
     * Find active installations whose extension_points contain the exact slot name.
     */
    @Query(value = """
        SELECT ai.* FROM app_installations ai
        WHERE ai.org_id = :orgId
          AND ai.status = 'active'
          AND ai.extension_points @> CAST(:slotJson AS jsonb)
        """, nativeQuery = true)
    List<AppInstallation> findByOrgIdAndExtensionPoint(
            @Param("orgId") String orgId,
            @Param("slotJson") String slotJson);

    /**
     * Find active installations that have a CDS Hooks discovery URL configured.
     */
    @Query(value = """
        SELECT ai.* FROM app_installations ai
        WHERE ai.org_id = :orgId
          AND ai.status = 'active'
          AND ai.cds_hooks_discovery_url IS NOT NULL
        """, nativeQuery = true)
    List<AppInstallation> findCdsHooksApps(@Param("orgId") String orgId);

    /**
     * Find active installations that support a specific CDS hook type.
     */
    @Query(value = """
        SELECT ai.* FROM app_installations ai
        WHERE ai.org_id = :orgId
          AND ai.status = 'active'
          AND ai.cds_hooks_discovery_url IS NOT NULL
          AND ai.supported_hooks @> CAST(:hookJson AS jsonb)
        """, nativeQuery = true)
    List<AppInstallation> findByOrgIdAndSupportedHook(
            @Param("orgId") String orgId,
            @Param("hookJson") String hookJson);
}
