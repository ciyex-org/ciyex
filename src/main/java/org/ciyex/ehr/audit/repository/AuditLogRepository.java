package org.ciyex.ehr.audit.repository;

import org.ciyex.ehr.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByOrgAliasOrderByCreatedAtDesc(String orgAlias, Pageable pageable);

    Page<AuditLog> findByOrgAliasAndUserIdOrderByCreatedAtDesc(String orgAlias, String userId, Pageable pageable);

    Page<AuditLog> findByOrgAliasAndPatientIdOrderByCreatedAtDesc(String orgAlias, Long patientId, Pageable pageable);

    Page<AuditLog> findByOrgAliasAndResourceTypeOrderByCreatedAtDesc(String orgAlias, String resourceType, Pageable pageable);

    Page<AuditLog> findByOrgAliasAndActionOrderByCreatedAtDesc(String orgAlias, String action, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.orgAlias = :org AND (" +
           "LOWER(a.userName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(a.resourceName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(a.patientName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(a.resourceType) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(a.action) LIKE LOWER(CONCAT('%',:q,'%'))" +
           ") ORDER BY a.createdAt DESC")
    Page<AuditLog> search(@Param("org") String orgAlias, @Param("q") String query, Pageable pageable);

    long countByOrgAliasAndActionAndCreatedAtAfter(String orgAlias, String action, LocalDateTime after);

    long countByOrgAliasAndCreatedAtAfter(String orgAlias, LocalDateTime after);

    @Query("SELECT DISTINCT a.resourceType FROM AuditLog a WHERE a.orgAlias = :org AND a.resourceType IS NOT NULL ORDER BY a.resourceType")
    java.util.List<String> findDistinctResourceTypesByOrgAlias(@Param("org") String orgAlias);
}
