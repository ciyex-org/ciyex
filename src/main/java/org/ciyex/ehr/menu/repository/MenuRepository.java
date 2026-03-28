package org.ciyex.ehr.menu.repository;

import org.ciyex.ehr.menu.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MenuRepository extends JpaRepository<Menu, UUID> {

    List<Menu> findByOrgIdAndLocation(String orgId, String location);

    Optional<Menu> findByCodeAndOrgIdAndPracticeTypeCode(String code, String orgId, String practiceTypeCode);

    /**
     * Find the best menu match for a code + practice type.
     * Returns practice-type-specific first, then default ('*'), all global (orgId='*').
     */
    @Query("SELECT m FROM Menu m WHERE m.code = :code AND m.orgId = '*' " +
           "AND m.practiceTypeCode IN (:practiceTypeCode, '*') " +
           "ORDER BY CASE WHEN m.practiceTypeCode = '*' THEN 1 ELSE 0 END")
    List<Menu> findGlobalByCodeForPracticeType(String code, String practiceTypeCode);

    boolean existsByCodeAndOrgId(String code, String orgId);

    void deleteByCodeAndOrgId(String code, String orgId);
}
