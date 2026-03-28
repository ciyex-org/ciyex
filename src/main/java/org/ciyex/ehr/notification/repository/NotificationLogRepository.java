package org.ciyex.ehr.notification.repository;

import org.ciyex.ehr.notification.entity.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    Page<NotificationLog> findByOrgAliasOrderByCreatedAtDesc(String orgAlias, Pageable pageable);

    List<NotificationLog> findByOrgAliasAndPatientIdOrderByCreatedAtDesc(String orgAlias, Long patientId);

    long countByOrgAliasAndStatus(String orgAlias, String status);

    @Query("SELECT l.status, COUNT(l) FROM NotificationLog l WHERE l.orgAlias = :org GROUP BY l.status")
    List<Object[]> countByStatus(@Param("org") String orgAlias);

    @Query("SELECT CAST(l.createdAt AS date), l.status, COUNT(l) FROM NotificationLog l " +
           "WHERE l.orgAlias = :org GROUP BY CAST(l.createdAt AS date), l.status " +
           "ORDER BY CAST(l.createdAt AS date) DESC")
    List<Object[]> countByDayAndStatus(@Param("org") String orgAlias);

    Page<NotificationLog> findByOrgAliasAndChannelTypeOrderByCreatedAtDesc(
            String orgAlias, String channelType, Pageable pageable);

    Page<NotificationLog> findByOrgAliasAndStatusOrderByCreatedAtDesc(
            String orgAlias, String status, Pageable pageable);
}
