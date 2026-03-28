package org.ciyex.ehr.priorauth.repository;

import org.ciyex.ehr.priorauth.entity.PriorAuthorization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PriorAuthRepository extends JpaRepository<PriorAuthorization, Long> {

    Page<PriorAuthorization> findByOrgAliasOrderByCreatedAtDesc(String orgAlias, Pageable pageable);

    List<PriorAuthorization> findByOrgAliasAndPatientIdOrderByRequestedDateDesc(String orgAlias, Long patientId);

    List<PriorAuthorization> findByOrgAliasAndStatusOrderByCreatedAtDesc(String orgAlias, String status);

    Page<PriorAuthorization> findByOrgAliasAndStatusOrderByCreatedAtDesc(String orgAlias, String status, Pageable pageable);

    long countByOrgAliasAndStatus(String orgAlias, String status);

    long countByOrgAlias(String orgAlias);

    @Query("SELECT p FROM PriorAuthorization p WHERE p.orgAlias = :org AND (" +
           "LOWER(p.patientName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(p.authNumber) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(p.procedureCode) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(p.procedureDescription) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(p.insuranceName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(p.diagnosisCode) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(p.providerName) LIKE LOWER(CONCAT('%',:q,'%'))" +
           ") ORDER BY p.createdAt DESC")
    List<PriorAuthorization> search(@Param("org") String orgAlias, @Param("q") String query);
}
