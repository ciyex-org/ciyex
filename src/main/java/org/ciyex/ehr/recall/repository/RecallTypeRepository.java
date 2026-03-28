package org.ciyex.ehr.recall.repository;

import org.ciyex.ehr.recall.entity.RecallType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecallTypeRepository extends JpaRepository<RecallType, Long> {

    List<RecallType> findByOrgAliasOrderByNameAsc(String orgAlias);

    List<RecallType> findByOrgAliasAndActiveOrderByNameAsc(String orgAlias, Boolean active);

    Optional<RecallType> findByOrgAliasAndCode(String orgAlias, String code);
}
