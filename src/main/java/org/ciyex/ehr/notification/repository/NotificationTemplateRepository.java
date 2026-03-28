package org.ciyex.ehr.notification.repository;

import org.ciyex.ehr.notification.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    List<NotificationTemplate> findByOrgAliasOrderByNameAsc(String orgAlias);

    Optional<NotificationTemplate> findByOrgAliasAndTemplateKeyAndChannelType(
            String orgAlias, String templateKey, String channelType);

    List<NotificationTemplate> findByOrgAliasAndChannelType(String orgAlias, String channelType);

    List<NotificationTemplate> findByOrgAliasAndIsActiveTrue(String orgAlias);

    @Query("SELECT t FROM NotificationTemplate t WHERE t.orgAlias = :org AND (" +
           "LOWER(t.name) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(t.templateKey) LIKE LOWER(CONCAT('%',:q,'%'))" +
           ") ORDER BY t.name ASC")
    List<NotificationTemplate> search(@Param("org") String orgAlias, @Param("q") String query);
}
