package org.ciyex.ehr.settings.repository;

import org.ciyex.ehr.settings.entity.UiColorConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UiColorConfigRepository extends JpaRepository<UiColorConfig, Long> {

    List<UiColorConfig> findByOrgId(String orgId);

    List<UiColorConfig> findByOrgIdAndCategory(String orgId, String category);

    Optional<UiColorConfig> findByOrgIdAndCategoryAndEntityKey(String orgId, String category, String entityKey);

    void deleteByOrgIdAndCategoryAndEntityKey(String orgId, String category, String entityKey);
}
