package com.qiaben.ciyex.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for AuditLog entities supporting ONC certification requirements
 * Provides comprehensive query capabilities for audit reporting and compliance
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    // === USER-BASED QUERIES ===
    
    /**
     * Find all audit logs for a specific user, ordered by most recent first
     * Required for user activity reporting per ONC § 170.315(d)(3)
     */
    List<AuditLog> findByUserIdOrderByEventTimeDesc(String userId);
    
    /**
     * Find audit logs for a user within a specific date range
     */
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId AND a.eventTime BETWEEN :startDate AND :endDate ORDER BY a.eventTime DESC")
    List<AuditLog> findByUserIdAndDateRange(@Param("userId") String userId, 
                                           @Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find recent failed login attempts for a user
     * Critical for security monitoring per ONC § 170.315(d)(1)
     */
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId AND a.actionType = 'LOGIN' AND a.success = false AND a.eventTime >= :since ORDER BY a.eventTime DESC")
    List<AuditLog> findFailedLoginAttempts(@Param("userId") String userId, @Param("since") LocalDateTime since);
    
    // === PATIENT DATA ACCESS QUERIES ===
    
    /**
     * Find all patient data access events for a specific patient
     * Critical for patient privacy auditing per ONC requirements
     */
    List<AuditLog> findByPatientIdOrderByEventTimeDesc(Long patientId);
    
    /**
     * Find patient data access by specific user and date range
     */
    @Query("SELECT a FROM AuditLog a WHERE a.patientId = :patientId AND a.userId = :userId AND a.eventTime BETWEEN :startDate AND :endDate ORDER BY a.eventTime DESC")
    List<AuditLog> findPatientAccessByUserAndDateRange(@Param("patientId") Long patientId, 
                                                       @Param("userId") String userId,
                                                       @Param("startDate") LocalDateTime startDate, 
                                                       @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find all users who accessed a specific patient's data within a time period
     */
    @Query("SELECT DISTINCT a.userId, a.userRole, COUNT(a) as accessCount FROM AuditLog a WHERE a.patientId = :patientId AND a.eventTime BETWEEN :startDate AND :endDate GROUP BY a.userId, a.userRole ORDER BY accessCount DESC")
    List<Object[]> findPatientAccessSummary(@Param("patientId") Long patientId, 
                                           @Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);
    
    // === ACTION TYPE QUERIES ===
    
    /**
     * Find audit logs by action type
     * Useful for analyzing specific types of operations
     */
    List<AuditLog> findByActionTypeOrderByEventTimeDesc(String actionType);
    
    /**
     * Find audit logs by entity type
     * Helps track access to specific types of resources
     */
    List<AuditLog> findByEntityTypeOrderByEventTimeDesc(String entityType);
    
    /**
     * Find failed operations across the system
     * Critical for security monitoring
     */
    List<AuditLog> findBySuccessFalseOrderByEventTimeDesc();
    
    /**
     * Find failed operations within a date range
     */
    @Query("SELECT a FROM AuditLog a WHERE a.success = false AND a.eventTime BETWEEN :startDate AND :endDate ORDER BY a.eventTime DESC")
    List<AuditLog> findFailedOperationsByDateRange(@Param("startDate") LocalDateTime startDate, 
                                                   @Param("endDate") LocalDateTime endDate);
    
    // === DATE RANGE QUERIES ===
    
    /**
     * Find audit logs within a specific date range
     * Essential for compliance reporting per ONC § 170.315(d)(3)
     */
    @Query("SELECT a FROM AuditLog a WHERE a.eventTime BETWEEN :startDate AND :endDate ORDER BY a.eventTime DESC")
    List<AuditLog> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                  @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find audit logs within a date range with pagination
     */
    @Query("SELECT a FROM AuditLog a WHERE a.eventTime BETWEEN :startDate AND :endDate ORDER BY a.eventTime DESC")
    Page<AuditLog> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                  @Param("endDate") LocalDateTime endDate, 
                                  Pageable pageable);
    
    // === SECURITY AND COMPLIANCE QUERIES ===
    
    /**
     * Find high-risk audit events
     * Critical for security monitoring
     */
    @Query("SELECT a FROM AuditLog a WHERE a.riskLevel IN ('HIGH', 'CRITICAL') ORDER BY a.eventTime DESC")
    List<AuditLog> findHighRiskEvents();
    
    /**
     * Find compliance-critical events
     * Required for ONC certification audit reports
     */
    List<AuditLog> findByComplianceCriticalTrueOrderByEventTimeDesc();
    
    /**
     * Find events by risk level
     */
    List<AuditLog> findByRiskLevelOrderByEventTimeDesc(AuditRiskLevel riskLevel);
    
    /**
     * Find PHI access events
     * Critical for HIPAA compliance
     */
    @Query("SELECT a FROM AuditLog a WHERE a.dataClassification IN ('PHI', 'SENSITIVE_PHI') ORDER BY a.eventTime DESC")
    List<AuditLog> findPhiAccessEvents();
    
    // === IP ADDRESS AND SECURITY QUERIES ===
    
    /**
     * Find audit logs by IP address
     * Important for security analysis and breach investigation
     */
    List<AuditLog> findByIpAddressOrderByEventTimeDesc(String ipAddress);
    
    /**
     * Find audit logs by IP address and date range
     */
    @Query("SELECT a FROM AuditLog a WHERE a.ipAddress = :ipAddress AND a.eventTime BETWEEN :startDate AND :endDate ORDER BY a.eventTime DESC")
    List<AuditLog> findByIpAddressAndDateRange(@Param("ipAddress") String ipAddress, 
                                              @Param("startDate") LocalDateTime startDate, 
                                              @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find multiple user sessions from same IP
     * Potential security concern
     */
    @Query("SELECT a.ipAddress, COUNT(DISTINCT a.userId) as userCount FROM AuditLog a WHERE a.eventTime >= :since GROUP BY a.ipAddress HAVING COUNT(DISTINCT a.userId) > 1 ORDER BY userCount DESC")
    List<Object[]> findMultiUserIpAddresses(@Param("since") LocalDateTime since);
    
    /**
     * Find audit logs by user and IP address
     */
    List<AuditLog> findByUserIdAndIpAddressOrderByEventTimeDesc(String userId, String ipAddress);
    
    // === ENDPOINT AND API QUERIES ===
    
    /**
     * Find audit logs by endpoint pattern
     */
    List<AuditLog> findByEndpointContainingOrderByEventTimeDesc(String endpointPattern);
    
    /**
     * Find most accessed endpoints
     */
    @Query("SELECT a.endpoint, COUNT(a) as accessCount FROM AuditLog a WHERE a.eventTime >= :since GROUP BY a.endpoint ORDER BY accessCount DESC")
    List<Object[]> findMostAccessedEndpoints(@Param("since") LocalDateTime since);
    
    // === SESSION QUERIES ===
    
    /**
     * Find audit logs by session ID
     * Useful for tracking user session activity
     */
    List<AuditLog> findBySessionIdOrderByEventTimeDesc(String sessionId);
    
    /**
     * Find concurrent sessions for a user
     */
    @Query("SELECT DISTINCT a.sessionId FROM AuditLog a WHERE a.userId = :userId AND a.eventTime BETWEEN :startDate AND :endDate AND a.sessionId IS NOT NULL")
    List<String> findUserSessionsInPeriod(@Param("userId") String userId, 
                                         @Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate);
    
    // === ORGANIZATIONAL QUERIES ===
    
    /**
     * Find audit logs by organization (for multi-tenant systems)
     */
    List<AuditLog> findByOrganizationIdOrderByEventTimeDesc(Long organizationId);
    
    /**
     * Find audit logs by organization and date range
     */
    @Query("SELECT a FROM AuditLog a WHERE a.organizationId = :orgId AND a.eventTime BETWEEN :startDate AND :endDate ORDER BY a.eventTime DESC")
    List<AuditLog> findByOrganizationAndDateRange(@Param("orgId") Long organizationId, 
                                                 @Param("startDate") LocalDateTime startDate, 
                                                 @Param("endDate") LocalDateTime endDate);
    
    // === STATISTICAL QUERIES ===
    
    /**
     * Count audit logs by action type within date range
     * Useful for generating compliance statistics
     */
    @Query("SELECT a.actionType, COUNT(a) FROM AuditLog a WHERE a.eventTime BETWEEN :startDate AND :endDate GROUP BY a.actionType ORDER BY COUNT(a) DESC")
    List<Object[]> countByActionTypeInDateRange(@Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate);
    
    /**
     * Count patient accesses by user role
     */
    @Query("SELECT a.userRole, COUNT(DISTINCT a.patientId) FROM AuditLog a WHERE a.patientId IS NOT NULL AND a.eventTime BETWEEN :startDate AND :endDate GROUP BY a.userRole")
    List<Object[]> countPatientAccessByRole(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get audit log count by date for trend analysis
     */
    @Query("SELECT DATE(a.eventTime), COUNT(a) FROM AuditLog a WHERE a.eventTime BETWEEN :startDate AND :endDate GROUP BY DATE(a.eventTime) ORDER BY DATE(a.eventTime)")
    List<Object[]> getAuditCountByDate(@Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate);
    
    // === PAGINATION SUPPORT ===
    
    /**
     * Get all audit logs with pagination
     * Required for large-scale audit log viewing
     */
    Page<AuditLog> findAllByOrderByEventTimeDesc(Pageable pageable);
    
    /**
     * Get compliance-critical events with pagination
     */
    Page<AuditLog> findByComplianceCriticalTrueOrderByEventTimeDesc(Pageable pageable);
    
    /**
     * Get PHI access events with pagination
     */
    @Query("SELECT a FROM AuditLog a WHERE a.dataClassification IN ('PHI', 'SENSITIVE_PHI') ORDER BY a.eventTime DESC")
    Page<AuditLog> findPhiAccessEvents(Pageable pageable);
}