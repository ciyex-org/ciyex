package org.ciyex.ehr.notification.repository;

import org.ciyex.ehr.notification.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
    List<NotificationPreference> findByOrgAlias(String orgAlias);

    Optional<NotificationPreference> findByOrgAliasAndEventType(String orgAlias, String eventType);
}
