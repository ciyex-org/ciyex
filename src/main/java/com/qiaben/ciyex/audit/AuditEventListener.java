package com.qiaben.ciyex.audit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Async;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Real-time audit event listener for security monitoring and compliance alerts
 * 
 * This component monitors audit events in real-time to detect security threats
 * and compliance violations as required by ONC certification.
 */
@Component
@Slf4j
public class AuditEventListener {

    @Autowired
    private AuditLogRepository auditLogRepository;
    
    @Autowired
    private AuditConfiguration auditConfiguration;
    
    // You would inject your notification service here
    // @Autowired
    // private NotificationService notificationService;

    /**
     * Handle failed login attempts
     * Critical for security monitoring per ONC § 170.315(d)(1)
     */
    @EventListener
    @Async
    public void handleFailedLoginEvent(AuditLog auditLog) {
        if (!"LOGIN".equals(auditLog.getActionType()) || auditLog.getSuccess()) {
            return;
        }
        
        log.warn("Failed login attempt detected: User={}, IP={}, Time={}", 
                auditLog.getUserId(), auditLog.getIpAddress(), auditLog.getEventTime());
        
        // Check for multiple failed attempts
        if (auditConfiguration.isEnableAlerts()) {
            checkForSuspiciousLoginActivity(auditLog);
        }
    }

    /**
     * Handle high-risk audit events
     * Triggers immediate alerts for critical security events
     */
    @EventListener
    @Async
    public void handleHighRiskEvent(AuditLog auditLog) {
        if (auditLog.getRiskLevel() != AuditRiskLevel.HIGH && 
            auditLog.getRiskLevel() != AuditRiskLevel.CRITICAL) {
            return;
        }
        
        log.warn("High-risk audit event detected: Action={}, Entity={}, User={}, Risk={}", 
                auditLog.getActionType(), auditLog.getEntityType(), 
                auditLog.getUserId(), auditLog.getRiskLevel());
        
        // Send immediate alert for critical events
        if (auditLog.getRiskLevel() == AuditRiskLevel.CRITICAL) {
            sendCriticalSecurityAlert(auditLog);
        }
    }

    /**
     * Handle patient data access events
     * Monitors for unusual patient data access patterns
     */
    @EventListener
    @Async
    public void handlePatientDataAccess(AuditLog auditLog) {
        if (auditLog.getPatientId() == null) {
            return;
        }
        
        log.debug("Patient data access: Patient={}, User={}, Action={}", 
                auditLog.getPatientId(), auditLog.getUserId(), auditLog.getActionType());
        
        // Check for unusual access patterns
        if (auditConfiguration.isEnableComplianceMonitoring()) {
            checkForUnusualPatientAccess(auditLog);
        }
    }

    /**
     * Handle break-glass access events
     * Critical security events requiring immediate attention
     */
    @EventListener
    @Async
    public void handleBreakGlassAccess(AuditLog auditLog) {
        if (!"BREAK_GLASS".equals(auditLog.getActionType())) {
            return;
        }
        
        log.error("BREAK GLASS ACCESS DETECTED: User={}, Patient={}, IP={}, Time={}", 
                auditLog.getUserId(), auditLog.getPatientId(), 
                auditLog.getIpAddress(), auditLog.getEventTime());
        
        // Immediate notification for break-glass access
        sendBreakGlassAlert(auditLog);
    }

    /**
     * Handle bulk data export events
     * Monitor for potential data breaches
     */
    @EventListener
    @Async
    public void handleDataExportEvent(AuditLog auditLog) {
        if (!"EXPORT".equals(auditLog.getActionType())) {
            return;
        }
        
        log.warn("Data export detected: User={}, Entity={}, IP={}", 
                auditLog.getUserId(), auditLog.getEntityType(), auditLog.getIpAddress());
        
        // Check for unusual export patterns
        checkForSuspiciousExportActivity(auditLog);
    }

    /**
     * Handle configuration changes
     * Monitor for unauthorized system modifications
     */
    @EventListener
    @Async
    public void handleConfigurationChange(AuditLog auditLog) {
        if (!"CONFIG_CHANGE".equals(auditLog.getActionType())) {
            return;
        }
        
        log.warn("System configuration change: User={}, Config={}, IP={}", 
                auditLog.getUserId(), auditLog.getEntityId(), auditLog.getIpAddress());
        
        // Send alert for configuration changes
        sendConfigurationChangeAlert(auditLog);
    }

    // ============================================================================
    // PRIVATE HELPER METHODS
    // ============================================================================

    private void checkForSuspiciousLoginActivity(AuditLog currentEvent) {
        LocalDateTime timeWindow = LocalDateTime.now()
                .minusMinutes(auditConfiguration.getFailedLoginTimeWindowMinutes());
        
        List<AuditLog> recentFailedLogins = auditLogRepository.findFailedLoginAttempts(
                currentEvent.getUserId(), timeWindow);
        
        if (recentFailedLogins.size() >= auditConfiguration.getFailedLoginAlertThreshold()) {
            log.error("SECURITY ALERT: Multiple failed login attempts detected for user {} from IP {}. " +
                    "Count: {} in {} minutes", 
                    currentEvent.getUserId(), currentEvent.getIpAddress(), 
                    recentFailedLogins.size(), auditConfiguration.getFailedLoginTimeWindowMinutes());
            
            sendSecurityAlert("Multiple Failed Login Attempts", 
                    String.format("User %s has %d failed login attempts from IP %s", 
                            currentEvent.getUserId(), recentFailedLogins.size(), 
                            currentEvent.getIpAddress()));
        }
    }

    private void checkForUnusualPatientAccess(AuditLog auditLog) {
        // Check if user is accessing patients outside their normal pattern
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        
        List<AuditLog> recentPatientAccess = auditLogRepository.findByUserIdAndDateRange(
                auditLog.getUserId(), last24Hours, LocalDateTime.now());
        
        long uniquePatients = recentPatientAccess.stream()
                .filter(log -> log.getPatientId() != null)
                .mapToLong(AuditLog::getPatientId)
                .distinct()
                .count();
        
        // Alert if user accessed more than 50 patients in 24 hours (configurable threshold)
        if (uniquePatients > 50) {
            log.warn("COMPLIANCE ALERT: User {} accessed {} unique patients in 24 hours", 
                    auditLog.getUserId(), uniquePatients);
            
            sendComplianceAlert("Unusual Patient Access Pattern", 
                    String.format("User %s accessed %d unique patients in 24 hours", 
                            auditLog.getUserId(), uniquePatients));
        }
    }

    private void checkForSuspiciousExportActivity(AuditLog auditLog) {
        // Check for multiple exports in short time frame
        LocalDateTime lastHour = LocalDateTime.now().minusHours(1);
        
        List<AuditLog> recentExports = auditLogRepository.findByUserIdAndDateRange(
                auditLog.getUserId(), lastHour, LocalDateTime.now())
                .stream()
                .filter(log -> "EXPORT".equals(log.getActionType()))
                .toList();
        
        if (recentExports.size() > 3) { // More than 3 exports in an hour
            log.error("SECURITY ALERT: Suspicious export activity detected for user {} from IP {}. " +
                    "Export count: {} in last hour", 
                    auditLog.getUserId(), auditLog.getIpAddress(), recentExports.size());
            
            sendSecurityAlert("Suspicious Data Export Activity", 
                    String.format("User %s performed %d data exports in the last hour", 
                            auditLog.getUserId(), recentExports.size()));
        }
    }

    private void sendCriticalSecurityAlert(AuditLog auditLog) {
        String message = String.format(
                "CRITICAL SECURITY EVENT DETECTED\n" +
                "Action: %s\n" +
                "User: %s (%s)\n" +
                "Entity: %s\n" +
                "IP Address: %s\n" +
                "Time: %s\n" +
                "Description: %s",
                auditLog.getActionType(), auditLog.getUserId(), auditLog.getUserRole(),
                auditLog.getEntityType(), auditLog.getIpAddress(), 
                auditLog.getEventTime(), auditLog.getDescription());
        
        log.error("CRITICAL SECURITY ALERT: {}", message);
        
        // Here you would integrate with your notification system
        // notificationService.sendCriticalAlert(message);
    }

    private void sendBreakGlassAlert(AuditLog auditLog) {
        String message = String.format(
                "BREAK GLASS ACCESS ALERT\n" +
                "User: %s (%s)\n" +
                "Patient ID: %s\n" +
                "IP Address: %s\n" +
                "Time: %s\n" +
                "Reason: %s",
                auditLog.getUserId(), auditLog.getUserRole(), auditLog.getPatientId(),
                auditLog.getIpAddress(), auditLog.getEventTime(), auditLog.getDetails());
        
        log.error("BREAK GLASS ALERT: {}", message);
        
        // Immediate notification required for break-glass access
        // notificationService.sendImmediateAlert(message);
    }

    private void sendConfigurationChangeAlert(AuditLog auditLog) {
        String message = String.format(
                "SYSTEM CONFIGURATION CHANGE\n" +
                "User: %s (%s)\n" +
                "Configuration: %s\n" +
                "IP Address: %s\n" +
                "Time: %s",
                auditLog.getUserId(), auditLog.getUserRole(), auditLog.getEntityId(),
                auditLog.getIpAddress(), auditLog.getEventTime());
        
        log.warn("CONFIG CHANGE ALERT: {}", message);
        
        // notificationService.sendConfigChangeAlert(message);
    }

    private void sendSecurityAlert(String title, String message) {
        log.error("SECURITY ALERT - {}: {}", title, message);
        
        // Integration point for your notification system
        // notificationService.sendSecurityAlert(title, message);
    }

    private void sendComplianceAlert(String title, String message) {
        log.warn("COMPLIANCE ALERT - {}: {}", title, message);
        
        // Integration point for your notification system
        // notificationService.sendComplianceAlert(title, message);
    }
}