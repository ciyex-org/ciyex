package org.ciyex.ehr.marketplace.repository;

import org.ciyex.ehr.marketplace.entity.AppUsageEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppUsageEventRepository extends JpaRepository<AppUsageEvent, UUID> {

    /** Find unreported events for batch reporting to marketplace */
    @Query("SELECT e FROM AppUsageEvent e WHERE e.reported = false ORDER BY e.recordedAt ASC")
    List<AppUsageEvent> findUnreported();

    /** Mark events as reported */
    @Modifying
    @Query("UPDATE AppUsageEvent e SET e.reported = true WHERE e.id IN :ids")
    void markReported(@Param("ids") List<UUID> ids);

    /** Count events per app for a given org within a time range */
    @Query(value = """
        SELECT app_slug, event_type, COUNT(*) as cnt
        FROM app_usage_events
        WHERE org_id = :orgId
          AND recorded_at >= :since
        GROUP BY app_slug, event_type
        ORDER BY app_slug, cnt DESC
        """, nativeQuery = true)
    List<Object[]> countByAppAndType(@Param("orgId") String orgId, @Param("since") LocalDateTime since);

    /** Count total events for an app for an org within a time range */
    @Query(value = """
        SELECT COUNT(*) FROM app_usage_events
        WHERE org_id = :orgId AND app_slug = :appSlug
          AND recorded_at >= :since
        """, nativeQuery = true)
    long countByOrgAndApp(@Param("orgId") String orgId, @Param("appSlug") String appSlug, @Param("since") LocalDateTime since);

    /** Daily usage trend for an app */
    @Query(value = """
        SELECT DATE(recorded_at) as usage_date, event_type, COUNT(*) as cnt
        FROM app_usage_events
        WHERE org_id = :orgId AND app_slug = :appSlug
          AND recorded_at >= :since
        GROUP BY DATE(recorded_at), event_type
        ORDER BY usage_date
        """, nativeQuery = true)
    List<Object[]> dailyTrend(@Param("orgId") String orgId, @Param("appSlug") String appSlug, @Param("since") LocalDateTime since);
}
