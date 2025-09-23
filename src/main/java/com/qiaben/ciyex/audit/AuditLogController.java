package com.qiaben.ciyex.audit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Audit Log Controller for EHR ONC Certification Requirements
 * 
 * Provides audit reporting capabilities required by:
 * - ONC § 170.315(d)(3) Audit report(s)
 * - HIPAA Security Rule § 164.312(b) Audit controls
 * 
 * This controller provides comprehensive audit reporting for compliance,
 * security monitoring, and breach investigation.
 */
@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR')")
public class AuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    // ============================================================================
    // BASIC AUDIT LOG RETRIEVAL
    // ============================================================================

    /**
     * Get audit logs with pagination
     * Required for ONC § 170.315(d)(3) audit report generation
     */
    @GetMapping
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        // Log audit log access for audit-the-auditor requirement
        auditLogService.logAuditLogAccess(getCurrentUserId(), getCurrentUserRole());
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> logs = auditLogService.getAllLogs(pageable);
        return ResponseEntity.ok(logs);
    }

    /**
     * Get audit logs by date range
     * Essential for compliance reporting and investigations
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<AuditLog>> getAuditLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        auditLogService.logAuditLogAccess(getCurrentUserId(), getCurrentUserRole());
        
        List<AuditLog> logs = auditLogService.getLogsByDateRange(startDate, endDate);
        return ResponseEntity.ok(logs);
    }

    /**
     * Get audit logs by specific user
     * Required for user activity monitoring per ONC requirements
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByUser(@PathVariable String userId) {
        auditLogService.logAuditLogAccess(getCurrentUserId(), getCurrentUserRole());
        
        List<AuditLog> logs = auditLogService.getLogsByUserId(userId);
        return ResponseEntity.ok(logs);
    }

    /**
     * Get audit logs by patient ID
     * Critical for patient data access monitoring per ONC § 170.315(d)(2)
     */
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR') or hasRole('PHYSICIAN')")
    public ResponseEntity<List<AuditLog>> getAuditLogsByPatient(@PathVariable Long patientId) {
        auditLogService.logAuditLogAccess(getCurrentUserId(), getCurrentUserRole());
        
        List<AuditLog> logs = auditLogService.getLogsByPatientId(patientId);
        return ResponseEntity.ok(logs);
    }

    // ============================================================================
    // SECURITY AND COMPLIANCE FOCUSED QUERIES
    // ============================================================================

    /**
     * Get failed operations
     * Critical for security monitoring and breach detection
     */
    @GetMapping("/failed-operations")
    public ResponseEntity<List<AuditLog>> getFailedOperations() {
        auditLogService.logAuditLogAccess(getCurrentUserId(), getCurrentUserRole());
        
        List<AuditLog> logs = auditLogService.getFailedOperations();
        return ResponseEntity.ok(logs);
    }

    /**
     * Get high-risk events
     * Essential for security incident monitoring
     */
    @GetMapping("/high-risk-events")
    public ResponseEntity<List<AuditLog>> getHighRiskEvents() {
        auditLogService.logAuditLogAccess(getCurrentUserId(), getCurrentUserRole());
        
        List<AuditLog> logs = auditLogService.getHighRiskEvents();
        return ResponseEntity.ok(logs);
    }

    /**
     * Get compliance-critical events
     * Required for ONC certification compliance monitoring
     */
    @GetMapping("/compliance-critical")
    public ResponseEntity<List<AuditLog>> getComplianceCriticalEvents() {
        auditLogService.logAuditLogAccess(getCurrentUserId(), getCurrentUserRole());
        
        List<AuditLog> logs = auditLogService.getComplianceCriticalEvents();
        return ResponseEntity.ok(logs);
    }

    /**
     * Get PHI access events
     * Critical for HIPAA compliance monitoring
     */
    @GetMapping("/phi-access")
    public ResponseEntity<List<AuditLog>> getPhiAccessEvents() {
        auditLogService.logAuditLogAccess(getCurrentUserId(), getCurrentUserRole());
        
        List<AuditLog> logs = auditLogService.getPhiAccessEvents();
        return ResponseEntity.ok(logs);
    }

    /**
     * Get failed login attempts
     * Critical for security monitoring per ONC § 170.315(d)(1)
     */
    @GetMapping("/failed-logins")
    public ResponseEntity<List<AuditLog>> getFailedLoginAttempts(
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "24") int hoursBack) {
        
        auditLogService.logAuditLogAccess(getCurrentUserId(), getCurrentUserRole());
        
        LocalDateTime since = LocalDateTime.now().minusHours(hoursBack);
        List<AuditLog> logs;
        
        if (userId != null) {
            logs = auditLogRepository.findFailedLoginAttempts(userId, since);
        } else {
            logs = auditLogRepository.findFailedOperationsByDateRange(since, LocalDateTime.now());
        }
        
        return ResponseEntity.ok(logs);
    }

    // ============================================================================
    // IP ADDRESS AND SECURITY ANALYSIS
    // ============================================================================

    /**
     * Get audit logs by IP address
     * Important for security analysis and breach investigation
     */
    @GetMapping("/ip-address/{ipAddress}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByIpAddress(@PathVariable String ipAddress) {
        auditLogService.logAuditLogAccess(getCurrentUserId(), getCurrentUserRole());
        
        List<AuditLog> logs = auditLogService.getLogsByIpAddress(ipAddress);
        return ResponseEntity.ok(logs);
    }

    /**
     * Get multiple user sessions from same IP
     * Potential security concern detection
     */
    @GetMapping("/multi-user-ips")
    public ResponseEntity<List<Object[]>> getMultiUserIpAddresses(
            @RequestParam(defaultValue = "24") int hoursBack) {
        
        auditLogService.logAuditLogAccess(getCurrentUserId(), getCurrentUserRole());
        
        LocalDateTime since = LocalDateTime.now().minusHours(hoursBack);
        List<Object[]> results = auditLogRepository.findMultiUserIpAddresses(since);
        return ResponseEntity.ok(results);
    }

    // ============================================================================
    // STATISTICAL AND ANALYTICAL REPORTS
    // ============================================================================

    /**
     * Get audit statistics by action type
     * Useful for compliance reporting and trend analysis
     */
    @GetMapping("/statistics/action-types")
    public ResponseEntity<List<Object[]>> getActionTypeStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        auditLogService.logAuditLogAccess(getCurrentUserId(), getCurrentUserRole());
        
        List<Object[]> stats = auditLogRepository.countByActionTypeInDateRange(startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get patient access statistics by user role
     * Important for access pattern analysis
     */
    @GetMapping("/statistics/patient-access-by-role")
    public ResponseEntity<List<Object[]>> getPatientAccessByRole(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        auditLogService.logAuditLogAccess(getCurrentUserId(), getCurrentUserRole());
        
        List<Object[]> stats = auditLogRepository.countPatientAccessByRole(startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get audit count by date for trend analysis
     * Useful for identifying unusual activity patterns
     */
    @GetMapping("/statistics/daily-trends")
    public ResponseEntity<List<Object[]>> getDailyAuditTrends(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        auditLogService.logAuditLogAccess(getCurrentUserId(), getCurrentUserRole());
        
        List<Object[]> trends = auditLogRepository.getAuditCountByDate(startDate, endDate);
        return ResponseEntity.ok(trends);
    }

    /**
     * Get most accessed endpoints
     * Useful for security and usage analysis
     */
    @GetMapping("/statistics/popular-endpoints")
    public ResponseEntity<List<Object[]>> getMostAccessedEndpoints(
            @RequestParam(defaultValue = "24") int hoursBack) {
        
        auditLogService.logAuditLogAccess(getCurrentUserId(), getCurrentUserRole());
        
        LocalDateTime since = LocalDateTime.now().minusHours(hoursBack);
        List<Object[]> endpoints = auditLogRepository.findMostAccessedEndpoints(since);
        return ResponseEntity.ok(endpoints);
    }

    // ============================================================================
    // PATIENT-SPECIFIC AUDIT REPORTS
    // ============================================================================

    /**
     * Get patient access summary
     * Shows all users who accessed a specific patient's data
     */
    @GetMapping("/patient/{patientId}/access-summary")
    public ResponseEntity<List<Object[]>> getPatientAccessSummary(
            @PathVariable Long patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        auditLogService.logAuditLogAccess(getCurrentUserId(), getCurrentUserRole());
        
        List<Object[]> summary = auditLogRepository.findPatientAccessSummary(patientId, startDate, endDate);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get detailed patient access log
     * Comprehensive view of all patient data interactions
     */
    @GetMapping("/patient/{patientId}/detailed")
    public ResponseEntity<List<AuditLog>> getDetailedPatientAccess(
            @PathVariable Long patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String userId) {
        
        auditLogService.logAuditLogAccess(getCurrentUserId(), getCurrentUserRole());
        
        List<AuditLog> logs;
        if (userId != null) {
            logs = auditLogRepository.findPatientAccessByUserAndDateRange(patientId, userId, startDate, endDate);
        } else {
            logs = auditLogRepository.findByPatientIdOrderByEventTimeDesc(patientId);
        }
        
        return ResponseEntity.ok(logs);
    }

    // ============================================================================
    // COMPLIANCE REPORTING
    // ============================================================================

    /**
     * Generate ONC compliance report
     * Comprehensive report for ONC certification requirements
     */
    @GetMapping("/compliance-report")
    public ResponseEntity<Map<String, Object>> generateComplianceReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        auditLogService.logAuditLogAccess(getCurrentUserId(), getCurrentUserRole());
        
        Map<String, Object> report = new HashMap<>();
        
        // Basic statistics
        List<AuditLog> allLogs = auditLogService.getLogsByDateRange(startDate, endDate);
        report.put("totalEvents", allLogs.size());
        
        // Compliance-critical events
        List<AuditLog> criticalEvents = auditLogService.getComplianceCriticalEvents();
        report.put("complianceCriticalEvents", criticalEvents.size());
        
        // PHI access events
        List<AuditLog> phiEvents = auditLogService.getPhiAccessEvents();
        report.put("phiAccessEvents", phiEvents.size());
        
        // Failed operations
        List<AuditLog> failedOps = auditLogService.getFailedOperations();
        report.put("failedOperations", failedOps.size());
        
        // High-risk events
        List<AuditLog> highRiskEvents = auditLogService.getHighRiskEvents();
        report.put("highRiskEvents", highRiskEvents.size());
        
        // Action type breakdown
        List<Object[]> actionStats = auditLogRepository.countByActionTypeInDateRange(startDate, endDate);
        report.put("actionTypeStatistics", actionStats);
        
        // User role access patterns
        List<Object[]> roleStats = auditLogRepository.countPatientAccessByRole(startDate, endDate);
        report.put("patientAccessByRole", roleStats);
        
        // Generate timestamp
        report.put("reportGeneratedAt", LocalDateTime.now());
        report.put("reportPeriodStart", startDate);
        report.put("reportPeriodEnd", endDate);
        
        return ResponseEntity.ok(report);
    }

    /**
     * Generate HIPAA compliance report
     * Focused on patient data access and privacy requirements
     */
    @GetMapping("/hipaa-report")
    public ResponseEntity<Map<String, Object>> generateHipaaReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        auditLogService.logAuditLogAccess(getCurrentUserId(), getCurrentUserRole());
        
        Map<String, Object> report = new HashMap<>();
        
        // PHI access events
        List<AuditLog> phiEvents = auditLogService.getPhiAccessEvents();
        report.put("totalPhiAccessEvents", phiEvents.size());
        
        // Patient access by role
        List<Object[]> roleAccess = auditLogRepository.countPatientAccessByRole(startDate, endDate);
        report.put("patientAccessByRole", roleAccess);
        
        // Failed access attempts
        List<AuditLog> failedAccess = auditLogRepository.findFailedOperationsByDateRange(startDate, endDate);
        report.put("failedAccessAttempts", failedAccess.size());
        
        // Break-glass access events
        List<AuditLog> periodLogs = auditLogService.getLogsByDateRange(startDate, endDate);
        long breakGlassCount = periodLogs.stream()
                .filter(log -> "BREAK_GLASS".equals(log.getActionType()))
                .count();
        report.put("breakGlassEvents", breakGlassCount);
        
        // Report metadata
        report.put("reportGeneratedAt", LocalDateTime.now());
        report.put("reportPeriodStart", startDate);
        report.put("reportPeriodEnd", endDate);
        report.put("reportType", "HIPAA_COMPLIANCE");
        
        return ResponseEntity.ok(report);
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    private String getCurrentUserId() {
        // This should integrate with your security context
        // Return the current authenticated user's ID
        return "AUDIT_USER"; // Placeholder
    }

    private String getCurrentUserRole() {
        // This should integrate with your security context
        // Return the current authenticated user's role
        return "AUDITOR"; // Placeholder
    }
}