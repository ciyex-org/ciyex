package org.ciyex.ehr.notification.repository;

import org.ciyex.ehr.notification.entity.NotificationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface NotificationConfigRepository extends JpaRepository<NotificationConfig, Long> {
    List<NotificationConfig> findByOrgAlias(String orgAlias);

    Optional<NotificationConfig> findByOrgAliasAndChannelType(String orgAlias, String channelType);
}
