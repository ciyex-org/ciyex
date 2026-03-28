package org.ciyex.ehr.cds.repository;

import org.ciyex.ehr.cds.entity.CdsRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CdsRuleRepository extends JpaRepository<CdsRule, Long> {
    Page<CdsRule> findByOrgAlias(String orgAlias, Pageable pageable);
    List<CdsRule> findByOrgAliasAndRuleType(String orgAlias, String ruleType);
    List<CdsRule> findByOrgAliasAndIsActiveTrueAndTriggerEvent(String orgAlias, String triggerEvent);

    @Query("SELECT r FROM CdsRule r WHERE r.orgAlias = :org AND (" +
           "LOWER(r.name) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(r.description) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(r.ruleType) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(r.category) LIKE LOWER(CONCAT('%',:q,'%'))" +
           ") ORDER BY r.createdAt DESC")
    List<CdsRule> search(@Param("org") String orgAlias, @Param("q") String query);

    long countByOrgAlias(String orgAlias);

    long countByOrgAliasAndIsActiveTrue(String orgAlias);
}
