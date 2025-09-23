package com.qiaben.ciyex.audit;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Comprehensive Audit Log Service for EHR ONC Certification Requirements
 * 
 * This service implements audit logging requirements for ONC Health IT Certification:
 * - § 170.315(d)(1) Authentication, access control, and authorization
 * - § 170.315(d)(2) Auditable events and tamper-resistance  
 * - § 170.315(d)(3) Audit report(s)
 * - § 170.315(d)(9) Trusted connection
 * 
 * Key Features:
 * - Comprehensive patient data access logging
 * - User authentication and authorization tracking
 * - Clinical document access auditing
 * - Administrative action monitoring
 * - Security event logging
 * - HIPAA compliance support
 */
@Service
@Transactional
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private HttpServletRequest request;

    // ============================================================================
    // CORE AUDIT LOGGING METHODS
    // ============================================================================

    /**
     * Main method for logging events - used by AOP aspect
     * This is the core method that all other audit methods ultimately call
     */
    public void logEvent(String actionType, String entityType, String entityId,
                         String description, String details, String userId, String userRole,
                         String ipAddress, String endpoint) {
        AuditLog log = new AuditLog(userId, userRole, actionType, entityType, entityId,
                description, details, ipAddress, endpoint);
        
        // Set additional context
        log.setHttpMethod(request.getMethod());
        log.setUserAgent(request.getHeader("User-Agent"));
        log.setSessionId(request.getSession(false) != null ? request.getSession().getId() : null);
        
        // Determine risk level and compliance criticality
        determineRiskLevel(log);
        
        auditLogRepository.save(log);
    }

    /**
     * Simplified version for manual logging when needed
     */
    public void logEvent(String actionType, String entityType, String entityId, String description) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }

        String username = authentication.getName();
        String userRole = authentication.getAuthorities().iterator().next().getAuthority();
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        logEvent(actionType, entityType, entityId, description, null, username, userRole, ipAddress, endpoint);
    }

    /**
     * Log patient-related events with enhanced security tracking
     */
    public void logPatientEvent(String actionType, Long patientId, String entityType, String entityId,
                               String description, String details) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }

        String username = authentication.getName();
        String userRole = authentication.getAuthorities().iterator().next().getAuthority();
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        AuditLog log = new AuditLog(username, userRole, actionType, entityType, entityId, patientId,
                description, details, ipAddress, endpoint);
        
        // Patient events are always compliance critical
        log.setComplianceCritical(true);
        log.setDataClassification(DataClassification.PHI);
        log.setHttpMethod(request.getMethod());
        log.setUserAgent(request.getHeader("User-Agent"));
        log.setSessionId(request.getSession(false) != null ? request.getSession().getId() : null);
        
        // Determine appropriate risk level
        determineRiskLevel(log);
        
        auditLogRepository.save(log);
    }

    // ============================================================================
    // AUTHENTICATION AND AUTHORIZATION LOGGING (ONC § 170.315(d)(1))
    // ============================================================================

    /**
     * Log successful user login
     * Required by ONC § 170.315(d)(1) for authentication auditing
     */
    public void logLogin(String username, String role) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        AuditLog log = new AuditLog(username, role, "LOGIN", "SYSTEM", null,
                "User logged in successfully", null, ipAddress, endpoint);
        log.setHttpMethod(request.getMethod());
        log.setUserAgent(request.getHeader("User-Agent"));
        log.setSessionId(request.getSession().getId());
        log.setComplianceCritical(true);
        
        auditLogRepository.save(log);
    }

    /**
     * Log failed login attempt
     * Critical for security monitoring per ONC § 170.315(d)(1)
     */
    public void logFailedLogin(String username, String reason) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        AuditLog log = new AuditLog(username != null ? username : "UNKNOWN", "UNAUTHENTICATED", 
                "LOGIN", "SYSTEM", null, "Failed login attempt: " + reason, null, ipAddress, endpoint);
        log.markAsFailed(reason, 401);
        log.markAsHighRisk();
        log.setHttpMethod(request.getMethod());
        log.setUserAgent(request.getHeader("User-Agent"));
        
        auditLogRepository.save(log);
    }

    /**
     * Log user logout
     * Required by ONC § 170.315(d)(1) for session management auditing
     */
    public void logLogout(String username, String role) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        AuditLog log = new AuditLog(username, role, "LOGOUT", "SYSTEM", null,
                "User logged out", null, ipAddress, endpoint);
        log.setHttpMethod(request.getMethod());
        log.setUserAgent(request.getHeader("User-Agent"));
        log.setSessionId(request.getSession(false) != null ? request.getSession().getId() : null);
        
        auditLogRepository.save(log);
    }

    /**
     * Log password change
     * Important for security auditing
     */
    public void logPasswordChange(String username, String role) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        AuditLog log = new AuditLog(username, role, "PASSWORD_CHANGE", "USER", username,
                "User changed password", null, ipAddress, endpoint);
        log.markAsHighRisk();
        log.setComplianceCritical(true);
        
        auditLogRepository.save(log);
    }

    /**
     * Log privilege escalation or role changes
     * Critical for access control auditing per ONC § 170.315(d)(1)
     */
    public void logPrivilegeChange(String targetUser, String oldRole, String newRole, String adminUser, String adminRole) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        JSONObject details = new JSONObject();
        details.put("target_user", targetUser);
        details.put("old_role", oldRole);
        details.put("new_role", newRole);
        details.put("admin_user", adminUser);

        AuditLog log = new AuditLog(adminUser, adminRole, "PRIVILEGE_CHANGE", "USER", targetUser,
                "User privilege changed from " + oldRole + " to " + newRole, details.toString(), ipAddress, endpoint);
        log.markAsCritical();
        
        auditLogRepository.save(log);
    }

    // ============================================================================
    // PATIENT DATA ACCESS LOGGING (ONC § 170.315(d)(2))
    // ============================================================================

    /**
     * Log patient record access
     * Critical for ONC § 170.315(d)(2) patient data access auditing
     */
    public void logPatientAccess(Long patientId, String patientName, String userId, String userRole) {
        logPatientView(patientId, "PATIENT", patientName, userId, userRole);
    }

    /**
     * Log patient consent management
     * Required for consent tracking per ONC requirements
     */
    public void logPatientConsent(Long patientId, String action, String details, String userId, String userRole) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        JSONObject detailJson = new JSONObject();
        detailJson.put("patient_id", patientId);
        detailJson.put("action", action);
        if (details != null) detailJson.put("details", details);

        AuditLog log = new AuditLog(userId, userRole, "CONSENT", "PATIENT",
                patientId.toString(), patientId, "Patient consent " + action,
                detailJson.toString(), ipAddress, endpoint);
        log.markAsHighRisk();
        log.setConsentReference("CONSENT-" + patientId + "-" + System.currentTimeMillis());
        
        auditLogRepository.save(log);
    }

    /**
     * Log break-glass access to patient data
     * Critical security event requiring immediate attention
     */
    public void logBreakGlassAccess(Long patientId, String patientName, String reason, String userId, String userRole) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        JSONObject details = new JSONObject();
        details.put("patient_id", patientId);
        details.put("patient_name", patientName);
        details.put("break_glass_reason", reason);
        details.put("emergency_access", true);

        AuditLog log = new AuditLog(userId, userRole, "BREAK_GLASS", "PATIENT",
                patientId.toString(), patientId, "Emergency break-glass access to patient: " + patientName,
                details.toString(), ipAddress, endpoint);
        log.markAsCritical();
        
        auditLogRepository.save(log);
    }

    // ============================================================================
    // CLINICAL DATA ACCESS LOGGING
    // ============================================================================

    /**
     * Log vitals entry and access
     */
    public void logVitalsEntry(Long vitalsId, Long patientId, String patientName, String userId, String userRole) {
        logPatientEvent("CREATE", patientId, "VITALS", vitalsId.toString(),
                "Vitals recorded for " + patientName, null);
    }

    /**
     * Log lab order creation
     */
    public void logLabOrder(Long orderId, String orderType, Long patientId, String patientName, String userId, String userRole) {
        JSONObject details = new JSONObject();
        details.put("patient_id", patientId);
        details.put("patient_name", patientName);
        details.put("order_type", orderType);

        logPatientEvent("CREATE", patientId, "LAB_ORDER", orderId.toString(),
                orderType + " lab order created for " + patientName, details.toString());
    }

    /**
     * Log lab result viewing
     */
    public void logLabResultView(Long resultId, String testName, Long patientId, String patientName, String userId, String userRole) {
        JSONObject details = new JSONObject();
        details.put("patient_id", patientId);
        details.put("patient_name", patientName);
        details.put("test_name", testName);

        logPatientEvent("VIEW", patientId, "LAB_RESULT", resultId.toString(),
                "Viewed lab result: " + testName + " for " + patientName, details.toString());
    }

    /**
     * Log medication prescription
     */
    public void logMedicationPrescription(Long medicationId, String medicationName, Long patientId, String patientName, String userId, String userRole) {
        JSONObject details = new JSONObject();
        details.put("patient_id", patientId);
        details.put("patient_name", patientName);
        details.put("medication_name", medicationName);

        logPatientEvent("PRESCRIBE", patientId, "MEDICATION", medicationId.toString(),
                "Prescribed " + medicationName + " for " + patientName, details.toString());
    }

    /**
     * Log allergy information updates
     */
    public void logAllergyUpdate(Long patientId, String patientName, String allergy,
                                 String reaction, String action, String userId, String userRole) {
        JSONObject details = new JSONObject();
        details.put("patient_id", patientId);
        details.put("patient_name", patientName);
        details.put("allergy", allergy);
        details.put("reaction", reaction);

        logPatientEvent(action, patientId, "ALLERGY", null,
                "Allergy " + action.toLowerCase() + ": " + allergy + " for " + patientName, details.toString());
    }

    /**
     * Log immunization administration
     */
    public void logImmunization(Long patientId, String patientName, String vaccine, String userId, String userRole) {
        JSONObject details = new JSONObject();
        details.put("patient_id", patientId);
        details.put("patient_name", patientName);
        details.put("vaccine", vaccine);

        logPatientEvent("ADMINISTER", patientId, "IMMUNIZATION", null,
                vaccine + " administered to " + patientName, details.toString());
    }

    // ============================================================================
    // DOCUMENT AND MESSAGING LOGGING
    // ============================================================================

    /**
     * Log clinical document access
     * Required for document access auditing per ONC requirements
     */
    public void logDocumentAccess(Long documentId, String docName, String docType,
                                  Long patientId, String action, String userId, String userRole) {
        JSONObject details = new JSONObject();
        details.put("patient_id", patientId);
        details.put("document_name", docName);
        details.put("document_type", docType);

        logPatientEvent(action, patientId, "DOCUMENT", documentId.toString(),
                docType + " document " + action.toLowerCase() + ": " + docName, details.toString());
    }

    /**
     * Log secure messaging
     */
    public void logMessageSend(Long messageId, String senderName, String recipientName,
                               String userId, String userRole) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        JSONObject details = new JSONObject();
        details.put("sender", senderName);
        details.put("recipient", recipientName);

        AuditLog log = new AuditLog(userId, userRole, "SEND", "MESSAGE",
                messageId.toString(), "Sent secure message to " + recipientName,
                details.toString(), ipAddress, endpoint);
        log.setComplianceCritical(true);
        log.setDataClassification(DataClassification.PHI);
        
        auditLogRepository.save(log);
    }

    // ============================================================================
    // APPOINTMENT AND ENCOUNTER LOGGING
    // ============================================================================

    /**
     * Log appointment booking
     */
    public void logAppointmentBooking(Long appointmentId, String patientName, String doctorName,
                                      String userId, String userRole) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        JSONObject details = new JSONObject();
        details.put("patient", patientName);
        details.put("doctor", doctorName);

        AuditLog log = new AuditLog(userId, userRole, "CREATE", "APPOINTMENT",
                appointmentId.toString(), "Booked appointment with Dr. " + doctorName,
                details.toString(), ipAddress, endpoint);
        log.setComplianceCritical(true);
        
        auditLogRepository.save(log);
    }

    /**
     * Log encounter creation
     */
    public void logEncounter(Long encounterId, String encounterType, Long patientId,
                             String patientName, String userId, String userRole) {
        JSONObject details = new JSONObject();
        details.put("patient_id", patientId);
        details.put("patient_name", patientName);
        details.put("encounter_type", encounterType);

        logPatientEvent("CREATE", patientId, "ENCOUNTER", encounterId.toString(),
                encounterType + " encounter created for " + patientName, details.toString());
    }

    // ============================================================================
    // TELEHEALTH AND PROVIDER LOGGING
    // ============================================================================

    /**
     * Log telehealth session
     */
    public void logTelehealthSession(Long sessionId, Long patientId, String patientName,
                                     Long providerId, String providerName, String userId, String userRole) {
        JSONObject details = new JSONObject();
        details.put("patient_id", patientId);
        details.put("patient_name", patientName);
        details.put("provider_id", providerId);
        details.put("provider_name", providerName);

        logPatientEvent("TELEHEALTH", patientId, "SESSION", sessionId.toString(),
                "Telehealth session between " + patientName + " and " + providerName, details.toString());
    }

    /**
     * Log provider data access
     */
    public void logProviderAccess(Long providerId, String providerName, String userId, String userRole) {
        logGenericView(providerId, "PROVIDER", providerName, userId, userRole);
    }

    // ============================================================================
    // BILLING AND FINANCIAL LOGGING
    // ============================================================================

    /**
     * Log billing actions
     */
    public void logBillingAction(Long invoiceId, String action, Double amount,
                                 Long patientId, String userId, String userRole) {
        JSONObject details = new JSONObject();
        details.put("patient_id", patientId);
        details.put("amount", amount);
        details.put("action", action);

        String description = "Billing action: " + action + " on invoice #" + invoiceId;
        if (amount != null) {
            description += " (Amount: $" + amount + ")";
        }

        logPatientEvent(action, patientId, "INVOICE", invoiceId.toString(),
                description, details.toString());
    }

    // ============================================================================
    // ADMINISTRATIVE AND SYSTEM LOGGING
    // ============================================================================

    /**
     * Log system configuration changes
     * Critical for system security auditing
     */
    public void logConfigurationChange(String configType, String configName, String oldValue, String newValue, String userId, String userRole) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        JSONObject details = new JSONObject();
        details.put("config_type", configType);
        details.put("config_name", configName);
        details.put("old_value", oldValue);
        details.put("new_value", newValue);

        AuditLog log = new AuditLog(userId, userRole, "CONFIG_CHANGE", "SYSTEM", configName,
                "Configuration changed: " + configName, details.toString(), ipAddress, endpoint);
        log.markAsCritical();
        
        auditLogRepository.save(log);
    }

    /**
     * Log data export operations
     * Critical for data breach prevention
     */
    public void logDataExport(String exportType, Integer recordCount, String exportFormat, String userId, String userRole) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        JSONObject details = new JSONObject();
        details.put("export_type", exportType);
        details.put("record_count", recordCount);
        details.put("export_format", exportFormat);

        AuditLog log = new AuditLog(userId, userRole, "EXPORT", "DATA", null,
                "Data export: " + exportType + " (" + recordCount + " records)", 
                details.toString(), ipAddress, endpoint);
        log.markAsHighRisk();
        log.setComplianceCritical(true);
        
        auditLogRepository.save(log);
    }

    /**
     * Log audit log access
     * Required for audit-the-auditor capability per ONC § 170.315(d)(3)
     */
    public void logAuditLogAccess(String userId, String userRole) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        AuditLog log = new AuditLog(userId, userRole, "VIEW", "AUDIT_LOG",
                null, "Audit log was accessed", null, ipAddress, endpoint);
        log.setComplianceCritical(true);
        
        auditLogRepository.save(log);
    }

    // ============================================================================
    // GENERIC CRUD OPERATIONS
    // ============================================================================

    /**
     * Generic view logging
     */
    public void logGenericView(Long entityId, String entityType, String entityName,
                        String userId, String userRole) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        JSONObject details = new JSONObject();
        details.put("entity_name", entityName);

        AuditLog log = new AuditLog(userId, userRole, "VIEW", entityType,
                entityId.toString(), "Viewed " + entityType.toLowerCase() + ": " + entityName,
                details.toString(), ipAddress, endpoint);
        
        auditLogRepository.save(log);
    }

    /**
     * Generic view logging for patient-related entities
     */
    public void logPatientView(Long entityId, String entityType, String entityName,
                              String userId, String userRole) {
        logPatientEvent("VIEW", entityId, entityType, entityId.toString(),
                "Viewed " + entityType.toLowerCase() + ": " + entityName, null);
    }

    public void logCreate(Long entityId, String entityType, String entityName,
                          String userId, String userRole) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        JSONObject details = new JSONObject();
        details.put("entity_name", entityName);

        AuditLog log = new AuditLog(userId, userRole, "CREATE", entityType,
                entityId.toString(), "Created " + entityType.toLowerCase() + ": " + entityName,
                details.toString(), ipAddress, endpoint);
        
        auditLogRepository.save(log);
    }

    public void logUpdate(Long entityId, String entityType, String entityName,
                          String userId, String userRole) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        JSONObject details = new JSONObject();
        details.put("entity_name", entityName);

        AuditLog log = new AuditLog(userId, userRole, "UPDATE", entityType,
                entityId.toString(), "Updated " + entityType.toLowerCase() + ": " + entityName,
                details.toString(), ipAddress, endpoint);
        
        auditLogRepository.save(log);
    }

    public void logDelete(Long entityId, String entityType, String entityName,
                          String userId, String userRole) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        JSONObject details = new JSONObject();
        details.put("entity_name", entityName);

        AuditLog log = new AuditLog(userId, userRole, "DELETE", entityType,
                entityId.toString(), "Deleted " + entityType.toLowerCase() + ": " + entityName,
                details.toString(), ipAddress, endpoint);
        log.markAsHighRisk(); // Deletions are higher risk
        
        auditLogRepository.save(log);
    }

    // ============================================================================
    // QUERY METHODS FOR AUDIT REPORTS (ONC § 170.315(d)(3))
    // ============================================================================

    public List<AuditLog> getLogsByUserId(String userId) {
        return auditLogRepository.findByUserIdOrderByEventTimeDesc(userId);
    }

    public List<AuditLog> getLogsByActionType(String actionType) {
        return auditLogRepository.findByActionTypeOrderByEventTimeDesc(actionType);
    }

    public List<AuditLog> getLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.findByDateRange(startDate, endDate);
    }

    public List<AuditLog> getLogsByPatientId(Long patientId) {
        return auditLogRepository.findByPatientIdOrderByEventTimeDesc(patientId);
    }

    public List<AuditLog> getLogsByIpAddress(String ipAddress) {
        return auditLogRepository.findByIpAddressOrderByEventTimeDesc(ipAddress);
    }

    public List<AuditLog> getLogsByEndpoint(String endpoint) {
        return auditLogRepository.findByEndpointContainingOrderByEventTimeDesc(endpoint);
    }

    public List<AuditLog> getLogsByUserIdAndIpAddress(String userId, String ipAddress) {
        return auditLogRepository.findByUserIdAndIpAddressOrderByEventTimeDesc(userId, ipAddress);
    }

    public List<AuditLog> getLogsByIpAddressAndDateRange(String ipAddress, LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.findByIpAddressAndDateRange(ipAddress, startDate, endDate);
    }

    public List<AuditLog> getFailedOperations() {
        return auditLogRepository.findBySuccessFalseOrderByEventTimeDesc();
    }

    public List<AuditLog> getHighRiskEvents() {
        return auditLogRepository.findHighRiskEvents();
    }

    public List<AuditLog> getComplianceCriticalEvents() {
        return auditLogRepository.findByComplianceCriticalTrueOrderByEventTimeDesc();
    }

    public List<AuditLog> getPhiAccessEvents() {
        return auditLogRepository.findPhiAccessEvents();
    }

    public Page<AuditLog> getAllLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByEventTimeDesc(pageable);
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    /**
     * Determine risk level based on audit event characteristics
     */
    private void determineRiskLevel(AuditLog log) {
        // Administrative actions are high risk
        if ("CONFIG_CHANGE".equals(log.getActionType()) || 
            "PRIVILEGE_CHANGE".equals(log.getActionType()) ||
            "BREAK_GLASS".equals(log.getActionType())) {
            log.setRiskLevel(AuditRiskLevel.CRITICAL);
            return;
        }
        
        // Failed operations are high risk
        if (!log.getSuccess()) {
            log.setRiskLevel(AuditRiskLevel.HIGH);
            return;
        }
        
        // Patient data access is medium risk by default
        if (log.getPatientId() != null) {
            log.setRiskLevel(AuditRiskLevel.MEDIUM);
            return;
        }
        
        // Delete operations are medium risk
        if ("DELETE".equals(log.getActionType())) {
            log.setRiskLevel(AuditRiskLevel.MEDIUM);
            return;
        }
        
        // Data exports are high risk
        if ("EXPORT".equals(log.getActionType())) {
            log.setRiskLevel(AuditRiskLevel.HIGH);
            return;
        }
        
        // Authentication events are medium risk
        if ("LOGIN".equals(log.getActionType()) || "LOGOUT".equals(log.getActionType())) {
            log.setRiskLevel(AuditRiskLevel.MEDIUM);
            return;
        }
        
        // Default to low risk
        log.setRiskLevel(AuditRiskLevel.LOW);
    }

    /**
     * Get client IP address with proxy support
     * Required for security auditing per ONC § 170.315(d)(9)
     */
    private String getClientIpAddress() {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Log failed operations with error details
     */
    public void logFailedEvent(String actionType, String entityType, String entityId,
                              String description, String errorMessage, String userId, String userRole,
                              String ipAddress, String endpoint, Integer responseStatus) {
        AuditLog log = new AuditLog(userId, userRole, actionType, entityType, entityId,
                description, null, ipAddress, endpoint);
        
        // Set additional context
        log.setHttpMethod(request.getMethod());
        log.setUserAgent(request.getHeader("User-Agent"));
        log.setSessionId(request.getSession(false) != null ? request.getSession().getId() : null);
        
        // Mark as failed
        log.markAsFailed(errorMessage, responseStatus);
        
        auditLogRepository.save(log);
    }
}