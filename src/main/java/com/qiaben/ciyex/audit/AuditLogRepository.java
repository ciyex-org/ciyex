package com.qiaben.ciyex.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    List<AuditLog> findByUserIdOrderByEventTimeDesc(String userId);

    List<AuditLog> findByActionTypeOrderByEventTimeDesc(String actionType);

    List<AuditLog> findByEntityTypeAndEntityIdOrderByEventTimeDesc(String entityType, String entityId);

    @Query("SELECT a FROM AuditLog a WHERE a.eventTime BETWEEN :startDate AND :endDate ORDER BY a.eventTime DESC")
    List<AuditLog> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    List<AuditLog> findByUserRoleOrderByEventTimeDesc(String userRole);

    List<AuditLog> findByIpAddressOrderByEventTimeDesc(String ipAddress);

    List<AuditLog> findByEndpointContainingOrderByEventTimeDesc(String endpoint);

    List<AuditLog> findByUserIdAndIpAddressOrderByEventTimeDesc(String userId, String ipAddress);

    @Query("SELECT a FROM AuditLog a WHERE a.ipAddress = :ipAddress AND a.eventTime BETWEEN :startDate AND :endDate ORDER BY a.eventTime DESC")
    List<AuditLog> findByIpAddressAndDateRange(@Param("ipAddress") String ipAddress,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    // For pagination
    org.springframework.data.domain.Page<AuditLog> findAll(org.springframework.data.domain.Pageable pageable);
}
