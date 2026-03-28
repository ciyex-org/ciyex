package org.ciyex.ehr.marketplace.repository;

import org.ciyex.ehr.marketplace.entity.AppUsageDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppUsageDailyRepository extends JpaRepository<AppUsageDaily, UUID> {

    Optional<AppUsageDaily> findByOrgIdAndAppSlugAndEventTypeAndUsageDate(
            String orgId, String appSlug, String eventType, LocalDate usageDate);

    @Query("SELECT d FROM AppUsageDaily d WHERE d.orgId = :orgId AND d.usageDate >= :since ORDER BY d.usageDate DESC")
    List<AppUsageDaily> findByOrgSince(@Param("orgId") String orgId, @Param("since") LocalDate since);

    @Query("SELECT d FROM AppUsageDaily d WHERE d.orgId = :orgId AND d.appSlug = :appSlug AND d.usageDate >= :since ORDER BY d.usageDate")
    List<AppUsageDaily> findByOrgAndAppSince(@Param("orgId") String orgId, @Param("appSlug") String appSlug, @Param("since") LocalDate since);

    @Query("SELECT d FROM AppUsageDaily d WHERE d.reported = false")
    List<AppUsageDaily> findUnreported();
}
