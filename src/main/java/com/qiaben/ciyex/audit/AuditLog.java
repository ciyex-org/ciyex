package com.qiaben.ciyex.audit;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Audit Log Entity for EHR ONC Certification Requirements
 * 
 * This entity captures all audit events required for ONC certification including:
 * - User authentication and access control events
 * - Patient data access and modifications 
 * - Clinical document access and changes
 * - Administrative actions
 * - System security events
 * 
 * ONC Requirements covered:
 * - § 170.315(d)(1) Authentication, access control, and authorization
 * - § 170.315(d)(2) Auditable events and tamper-resistance
 * - § 170.315(d)(3) Audit report(s)
 * - § 170.315(d)(9) Trusted connection
 */
@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_audit_user_id", columnList = "user_id"),
    @Index(name = "idx_audit_event_time", columnList = "event_time"),
    @Index(name = "idx_audit_action_type", columnList = "action_type"),
    @Index(name = "idx_audit_entity_type", columnList = "entity_type"),
    @Index(name = "idx_audit_patient_id", columnList = "patient_id"),
    @Index(name = "idx_audit_ip_address", columnList = "ip_address"),
    @Index(name = "idx_audit_session_id", columnList = "session_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Timestamp when the auditable event occurred
     * Required by ONC § 170.315(d)(2)
     */
    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;
    
    /**
     * User identifier performing the action
     * Required by ONC § 170.315(d)(2)
     */
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;
    
    /**
     * User's role/authority at time of action
     * Required for access control auditing per ONC § 170.315(d)(1)
     */
    @Column(name = "user_role", nullable = false, length = 50)
    private String userRole;
    
    /**
     * Session identifier for the user session
     * Helps track user activity across requests
     */
    @Column(name = "session_id", length = 255)
    private String sessionId;
    
    /**
     * Type of action performed (CREATE, READ, UPDATE, DELETE, LOGIN, LOGOUT, etc.)
     * Required by ONC § 170.315(d)(2)
     */
    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;
    
    /**
     * Type of entity/resource being accessed or modified
     * Required by ONC § 170.315(d)(2)
     */
    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;
    
    /**
     * Unique identifier of the specific entity instance
     * Required by ONC § 170.315(d)(2)
     */
    @Column(name = "entity_id", length = 100)
    private String entityId;
    
    /**
     * Patient ID if the action involves patient data
     * Critical for patient data access auditing per ONC requirements
     */
    @Column(name = "patient_id")
    private Long patientId;
    
    /**
     * Human-readable description of the auditable event
     * Required by ONC § 170.315(d)(2)
     */
    @Column(name = "description", nullable = false, length = 500)
    private String description;
    
    /**
     * Additional structured details about the event (JSON format)
     * Stores context-specific information for comprehensive auditing
     */
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;
    
    /**
     * Source IP address of the request
     * Required for security auditing per ONC § 170.315(d)(9)
     */
    @Column(name = "ip_address", length = 45) // IPv6 compatible
    private String ipAddress;
    
    /**
     * User agent string from the request
     * Helps identify the client application/browser
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    /**
     * API endpoint or URL accessed
     * Required by ONC § 170.315(d)(2)
     */
    @Column(name = "endpoint", length = 500)
    private String endpoint;
    
    /**
     * HTTP method used (GET, POST, PUT, DELETE, etc.)
     * Provides context for the type of operation
     */
    @Column(name = "http_method", length = 10)
    private String httpMethod;
    
    /**
     * Response status code
     * Helps track successful vs failed operations
     */
    @Column(name = "response_status")
    private Integer responseStatus;
    
    /**
     * Success flag for the operation
     * Quick indicator for filtering successful vs failed events
     */
    @Column(name = "success", nullable = false)
    private Boolean success = true;
    
    /**
     * Error message if the operation failed
     * Captures failure details for security monitoring
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    /**
     * Risk level of the auditable event (LOW, MEDIUM, HIGH, CRITICAL)
     * Helps prioritize security review and compliance monitoring
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    private AuditRiskLevel riskLevel = AuditRiskLevel.LOW;
    
    /**
     * Whether this event requires special attention for compliance
     * Flags events that are critical for ONC certification requirements
     */
    @Column(name = "compliance_critical", nullable = false)
    private Boolean complianceCritical = false;
    
    /**
     * Organization/tenant ID for multi-tenant systems
     * Ensures audit logs are properly isolated
     */
    @Column(name = "organization_id")
    private Long organizationId;
    
    /**
     * Data classification level of accessed information
     * Tracks access to sensitive patient data
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "data_classification")
    private DataClassification dataClassification;
    
    /**
     * Consent reference if patient consent was involved
     * Links to patient consent for data access validation
     */
    @Column(name = "consent_reference", length = 100)
    private String consentReference;
    
    // Constructors
    public AuditLog(String userId, String userRole, String actionType, String entityType, 
                   String entityId, String description, String details, String ipAddress, String endpoint) {
        this.eventTime = LocalDateTime.now();
        this.userId = userId;
        this.userRole = userRole;
        this.actionType = actionType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.description = description;
        this.details = details;
        this.ipAddress = ipAddress;
        this.endpoint = endpoint;
        this.success = true;
        this.riskLevel = AuditRiskLevel.LOW;
        this.complianceCritical = false;
    }
    
    /**
     * Constructor for patient-related audit events
     */
    public AuditLog(String userId, String userRole, String actionType, String entityType, 
                   String entityId, Long patientId, String description, String details, 
                   String ipAddress, String endpoint) {
        this(userId, userRole, actionType, entityType, entityId, description, details, ipAddress, endpoint);
        this.patientId = patientId;
        this.complianceCritical = true; // Patient data access is always compliance critical
        this.dataClassification = DataClassification.PHI;
    }
    
    /**
     * Mark this audit event as failed
     */
    public void markAsFailed(String errorMessage, Integer responseStatus) {
        this.success = false;
        this.errorMessage = errorMessage;
        this.responseStatus = responseStatus;
        this.riskLevel = AuditRiskLevel.HIGH; // Failed operations are higher risk
    }
    
    /**
     * Set high risk level for security-sensitive operations
     */
    public void markAsHighRisk() {
        this.riskLevel = AuditRiskLevel.HIGH;
        this.complianceCritical = true;
    }
    
    /**
     * Set critical risk level for administrative operations
     */
    public void markAsCritical() {
        this.riskLevel = AuditRiskLevel.CRITICAL;
        this.complianceCritical = true;
    }
}