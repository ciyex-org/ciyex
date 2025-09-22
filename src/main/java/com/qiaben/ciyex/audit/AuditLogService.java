package com.qiaben.ciyex.audit;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;

@Service
@Transactional
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private HttpServletRequest request;

    /**
     * Main method for logging events - used by AOP aspect
     */
    public void logEvent(String actionType, String entityType, String entityId,
                         String description, String details, String userId, String userRole,
                         String ipAddress, String endpoint) {
        AuditLog log = new AuditLog(userId, userRole, actionType, entityType, entityId,
                description, details, ipAddress, endpoint);
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

    // === AUTHENTICATION ===
    public void logLogin(String username, String role) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        AuditLog log = new AuditLog(username, role, "LOGIN", "SYSTEM", null,
                "User logged in", null, ipAddress, endpoint);
        auditLogRepository.save(log);
    }

    public void logLogout(String username, String role) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        AuditLog log = new AuditLog(username, role, "LOGOUT", "SYSTEM", null,
                "User logged out", null, ipAddress, endpoint);
        auditLogRepository.save(log);
    }

    // === APPOINTMENTS ===
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
        auditLogRepository.save(log);
    }

    // === PATIENTS ===
    public void logPatientAccess(Long patientId, String patientName, String userId, String userRole) {
        logView(patientId, "PATIENT", patientName, userId, userRole);
    }

    public void logPatientConsent(Long patientId, String action, String details, String userId, String userRole) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        JSONObject detailJson = new JSONObject();
        detailJson.put("patient_id", patientId);
        detailJson.put("action", action);
        if (details != null) detailJson.put("details", details);

        AuditLog log = new AuditLog(userId, userRole, "CONSENT", "PATIENT",
                patientId.toString(), "Patient consent " + action,
                detailJson.toString(), ipAddress, endpoint);
        auditLogRepository.save(log);
    }

    // === VITALS ===
    public void logVitalsEntry(Long vitalsId, Long patientId, String patientName, String userId, String userRole) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        JSONObject details = new JSONObject();
        details.put("patient_id", patientId);
        details.put("patient_name", patientName);

        AuditLog log = new AuditLog(userId, userRole, "CREATE", "VITAL",
                vitalsId.toString(), "Vitals entry recorded for " + patientName,
                details.toString(), ipAddress, endpoint);
        auditLogRepository.save(log);
    }

    // === LAB ORDERS ===
    public void logLabOrder(Long orderId, String orderType, Long patientId, String patientName, String userId, String userRole) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        JSONObject details = new JSONObject();
        details.put("patient_id", patientId);
        details.put("patient_name", patientName);
        details.put("order_type", orderType);

        AuditLog log = new AuditLog(userId, userRole, "CREATE", "LAB_ORDER",
                orderId.toString(), orderType + " lab order created for " + patientName,
                details.toString(), ipAddress, endpoint);
        auditLogRepository.save(log);
    }

    public void logLabResultView(Long resultId, String testName, Long patientId, String patientName, String userId, String userRole) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        JSONObject details = new JSONObject();
        details.put("patient_id", patientId);
        details.put("patient_name", patientName);
        details.put("test_name", testName);

        AuditLog log = new AuditLog(userId, userRole, "VIEW", "LAB_RESULT",
                resultId.toString(), "Viewed " + testName + " result for " + patientName,
                details.toString(), ipAddress, endpoint);
        auditLogRepository.save(log);
    }

    // === MESSAGING ===
    public void logMessageSend(Long messageId, String senderName, String recipientName,
                               String userId, String userRole) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        JSONObject details = new JSONObject();
        details.put("sender", senderName);
        details.put("recipient", recipientName);

        AuditLog log = new AuditLog(userId, userRole, "SEND", "MESSAGE",
                messageId.toString(), "Sent message to " + recipientName,
                details.toString(), ipAddress, endpoint);
        auditLogRepository.save(log);
    }

    public void logMessageReply(Long messageId, String senderName, String recipientName,
                                String userId, String userRole) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        JSONObject details = new JSONObject();
        details.put("sender", senderName);
        details.put("recipient", recipientName);

        AuditLog log = new AuditLog(userId, userRole, "REPLY", "MESSAGE",
                messageId.toString(), "Replied to message from " + recipientName,
                details.toString(), ipAddress, endpoint);
        auditLogRepository.save(log);
    }

    // === TELEHEALTH ===
    public void logTelehealthSession(Long sessionId, Long patientId, String patientName,
                                     Long providerId, String providerName, String userId, String userRole) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        JSONObject details = new JSONObject();
        details.put("patient_id", patientId);
        details.put("patient_name", patientName);
        details.put("provider_id", providerId);
        details.put("provider_name", providerName);

        AuditLog log = new AuditLog(userId, userRole, "START", "TELEHEALTH_SESSION",
                sessionId.toString(), "Telehealth session started with " + patientName,
                details.toString(), ipAddress, endpoint);
        auditLogRepository.save(log);
    }

    // === PROVIDERS ===
    public void logProviderAccess(Long providerId, String providerName, String userId, String userRole) {
        logView(providerId, "PROVIDER", providerName, userId, userRole);
    }

    // === REFERRAL PROVIDERS ===
    public void logReferral(Long referralId, String reason, Long fromProviderId, Long toProviderId,
                            Long patientId, String userId, String userRole) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        JSONObject details = new JSONObject();
        details.put("reason", reason);
        details.put("from_provider_id", fromProviderId);
        details.put("to_provider_id", toProviderId);
        details.put("patient_id", patientId);

        AuditLog log = new AuditLog(userId, userRole, "CREATE", "REFERRAL",
                referralId.toString(), "Referral created for patient ID: " + patientId,
                details.toString(), ipAddress, endpoint);
        auditLogRepository.save(log);
    }

    // === INSURANCE / COVERAGES ===
    public void logInsuranceVerification(Long patientId, String patientName, String company,
                                         String status, String userId, String userRole) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        JSONObject details = new JSONObject();
        details.put("patient_id", patientId);
        details.put("patient_name", patientName);
        details.put("insurance_company", company);
        details.put("verification_status", status);

        AuditLog log = new AuditLog(userId, userRole, "VERIFY", "INSURANCE",
                patientId.toString(), "Insurance verification for " + patientName + ": " + status,
                details.toString(), ipAddress, endpoint);
        auditLogRepository.save(log);
    }

    // === ALLERGY INTOLERANCES ===
    public void logAllergyUpdate(Long patientId, String patientName, String allergy,
                                 String reaction, String action, String userId, String userRole) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        JSONObject details = new JSONObject();
        details.put("patient_id", patientId);
        details.put("patient_name", patientName);
        details.put("allergy", allergy);
        details.put("reaction", reaction);

        AuditLog log = new AuditLog(userId, userRole, action, "ALLERGY",
                patientId.toString(), action + " allergy (" + allergy + ") for " + patientName,
                details.toString(), ipAddress, endpoint);
        auditLogRepository.save(log);
    }

    // === IMMUNIZATIONS ===
    public void logImmunization(Long patientId, String patientName, String vaccine, String userId, String userRole) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        JSONObject details = new JSONObject();
        details.put("patient_id", patientId);
        details.put("patient_name", patientName);
        details.put("vaccine", vaccine);

        AuditLog log = new AuditLog(userId, userRole, "ADMINISTER", "IMMUNIZATION",
                patientId.toString(), vaccine + " administered to " + patientName,
                details.toString(), ipAddress, endpoint);
        auditLogRepository.save(log);
    }

    // === DOCUMENTS ===
    public void logDocumentUpload(Long documentId, String docName, String docType,
                                  Long patientId, String userId, String userRole) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        JSONObject details = new JSONObject();
        details.put("patient_id", patientId);
        details.put("document_name", docName);
        details.put("document_type", docType);

        AuditLog log = new AuditLog(userId, userRole, "UPLOAD", "DOCUMENT",
                documentId.toString(), docType + " document uploaded: " + docName,
                details.toString(), ipAddress, endpoint);
        auditLogRepository.save(log);
    }

    // === REPORTS ===
    public void logReportGeneration(String reportType, String parameters, String userId, String userRole) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        JSONObject details = new JSONObject();
        details.put("report_type", reportType);
        if (parameters != null) details.put("parameters", parameters);

        AuditLog log = new AuditLog(userId, userRole, "GENERATE", "REPORT",
                null, reportType + " report generated",
                details.toString(), ipAddress, endpoint);
        auditLogRepository.save(log);
    }

    // === NOTIFICATIONS ===
    public void logNotificationSent(String notificationType, String recipient, String userId, String userRole) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        JSONObject details = new JSONObject();
        details.put("notification_type", notificationType);
        details.put("recipient", recipient);

        AuditLog log = new AuditLog(userId, userRole, "SEND", "NOTIFICATION",
                null, notificationType + " notification sent to " + recipient,
                details.toString(), ipAddress, endpoint);
        auditLogRepository.save(log);
    }

    // === ENCOUNTERS ===
    public void logEncounter(Long encounterId, String encounterType, Long patientId,
                             String patientName, String userId, String userRole) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        JSONObject details = new JSONObject();
        details.put("patient_id", patientId);
        details.put("patient_name", patientName);
        details.put("encounter_type", encounterType);

        AuditLog log = new AuditLog(userId, userRole, "CREATE", "ENCOUNTER",
                encounterId.toString(), encounterType + " encounter created for " + patientName,
                details.toString(), ipAddress, endpoint);
        auditLogRepository.save(log);
    }

    // === BILLING ===
    public void logBillingAction(Long invoiceId, String action, Double amount,
                                 Long patientId, String userId, String userRole) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        JSONObject details = new JSONObject();
        details.put("patient_id", patientId);
        details.put("amount", amount);
        details.put("action", action);

        AuditLog log = new AuditLog(userId, userRole, action, "INVOICE",
                invoiceId.toString(), "Billing action: " + action + " on invoice #" + invoiceId,
                details.toString(), ipAddress, endpoint);
        auditLogRepository.save(log);
    }

    // === HISTORY (AUDIT LOG ACCESS) ===
    public void logAuditLogAccess(String userId, String userRole) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        AuditLog log = new AuditLog(userId, userRole, "VIEW", "AUDIT_LOG",
                null, "Audit log was accessed",
                null, ipAddress, endpoint);
        auditLogRepository.save(log);
    }

    // === AI / ASSISTANT ===
    public void logAIAction(String action, String query, String resultSummary, String userId, String userRole) {
        String ipAddress = getClientIpAddress();
        String endpoint = request.getRequestURI();

        JSONObject details = new JSONObject();
        details.put("query", query);
        details.put("result_summary", resultSummary);

        AuditLog log = new AuditLog(userId, userRole, action, "AI_ASSISTANT",
                null, "AI assistant used for: " + action,
                details.toString(), ipAddress, endpoint);
        auditLogRepository.save(log);
    }

    // === GENERIC CRUD OPERATIONS ===
    public void logView(Long entityId, String entityType, String entityName,
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
        auditLogRepository.save(log);
    }

    // === QUERY METHODS ===
    public java.util.List<AuditLog> getLogsByUserId(String userId) {
        return auditLogRepository.findByUserIdOrderByEventTimeDesc(userId);
    }

    public java.util.List<AuditLog> getLogsByActionType(String actionType) {
        return auditLogRepository.findByActionTypeOrderByEventTimeDesc(actionType);
    }

    public java.util.List<AuditLog> getLogsByDateRange(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        return auditLogRepository.findByDateRange(startDate, endDate);
    }

    public java.util.List<AuditLog> getLogsByIpAddress(String ipAddress) {
        return auditLogRepository.findByIpAddressOrderByEventTimeDesc(ipAddress);
    }

    public java.util.List<AuditLog> getLogsByEndpoint(String endpoint) {
        return auditLogRepository.findByEndpointContainingOrderByEventTimeDesc(endpoint);
    }

    public java.util.List<AuditLog> getLogsByUserIdAndIpAddress(String userId, String ipAddress) {
        return auditLogRepository.findByUserIdAndIpAddressOrderByEventTimeDesc(userId, ipAddress);
    }

    public java.util.List<AuditLog> getLogsByIpAddressAndDateRange(String ipAddress, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        return auditLogRepository.findByIpAddressAndDateRange(ipAddress, startDate, endDate);
    }

    public org.springframework.data.domain.Page<AuditLog> getAllLogs(org.springframework.data.domain.Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    // === UTILITY METHODS ===
    private String getClientIpAddress() {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }

        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        return ipAddress;
    }
}