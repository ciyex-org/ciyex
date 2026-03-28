package org.ciyex.ehr.careplan.repository;

import org.ciyex.ehr.careplan.entity.CarePlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CarePlanRepository extends JpaRepository<CarePlan, Long> {

    List<CarePlan> findByOrgAliasAndPatientIdOrderByCreatedAtDesc(String orgAlias, Long patientId);

    List<CarePlan> findByOrgAliasAndStatusOrderByCreatedAtDesc(String orgAlias, String status);

    List<CarePlan> findByOrgAliasOrderByCreatedAtDesc(String orgAlias);

    @Query("SELECT c FROM CarePlan c WHERE c.orgAlias = :org AND (" +
           "LOWER(c.title) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(c.patientName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(c.authorName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(c.category) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "CAST(c.patientId AS text) = :q" +
           ") ORDER BY c.createdAt DESC")
    List<CarePlan> search(@Param("org") String orgAlias, @Param("q") String query);

    long countByOrgAliasAndStatus(String orgAlias, String status);

    long countByOrgAliasAndPatientIdAndStatus(String orgAlias, Long patientId, String status);

    long countByOrgAliasAndPatientId(String orgAlias, Long patientId);

    long countByOrgAlias(String orgAlias);

    Page<CarePlan> findByOrgAliasOrderByCreatedAtDesc(String orgAlias, Pageable pageable);

    Page<CarePlan> findByOrgAliasAndStatusOrderByCreatedAtDesc(String orgAlias, String status, Pageable pageable);
}
