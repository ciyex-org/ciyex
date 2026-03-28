package org.ciyex.ehr.tabconfig.repository;

import org.ciyex.ehr.tabconfig.entity.PracticeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PracticeTypeRepository extends JpaRepository<PracticeType, UUID> {
    List<PracticeType> findByActiveTrueAndOrgIdOrderByName(String orgId);

    Optional<PracticeType> findByCodeAndOrgId(String code, String orgId);

    @Query("SELECT p FROM PracticeType p WHERE p.active = true AND (p.orgId = :orgId OR p.orgId = '*') ORDER BY p.category, p.name")
    List<PracticeType> findAllForOrg(String orgId);

    void deleteByCodeAndOrgId(String code, String orgId);
}
