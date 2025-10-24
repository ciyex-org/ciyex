package com.qiaben.ciyex.service;

import com.qiaben.ciyex.entity.portal.PortalPendingUpdate;
import com.qiaben.ciyex.dto.integration.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Service for merging approved portal data into EHR tenant schemas
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantDataMergeService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Merge approved portal data into appropriate EHR tenant schema
     */
    @Transactional
    public void mergeApprovedData(Long orgId, PortalPendingUpdate update) {
        try {
            // Set tenant context
            String tenantSchema = "practice_" + orgId;
            RequestContext ctx = new RequestContext();
            ctx.setTenantName(tenantSchema);
            RequestContext.set(ctx);
            
            log.info("🔄 Merging approved data - Type: {}, Tenant: {}", update.getUpdateType(), tenantSchema);

            switch (update.getUpdateType()) {
                case "DEMOGRAPHICS":
                    mergeDemographicsData(tenantSchema, update);
                    break;
                case "INSURANCE":
                    mergeInsuranceData(tenantSchema, update);
                    break;
                case "BILLING":
                    mergeBillingData(tenantSchema, update);
                    break;
                case "EMERGENCY_CONTACT":
                    mergeEmergencyContactData(tenantSchema, update);
                    break;
                case "MESSAGING":
                    mergeMessagingData(tenantSchema, update);
                    break;
                default:
                    log.warn("⚠️ Unknown update type: {}", update.getUpdateType());
            }

            log.info("✅ Data merged successfully - Type: {}, Tenant: {}", update.getUpdateType(), tenantSchema);

        } catch (Exception e) {
            log.error("❌ Failed to merge approved data - Type: {}, OrgId: {}", 
                    update.getUpdateType(), orgId, e);
            throw new RuntimeException("Failed to merge approved data: " + e.getMessage());
        } finally {
            RequestContext.clear();
        }
    }

    /**
     * Merge demographics data into tenant patient table
     */
    private void mergeDemographicsData(String tenantSchema, PortalPendingUpdate update) {
        Map<String, Object> changes = update.getPayload();
        
        StringBuilder sql = new StringBuilder("UPDATE ").append(tenantSchema).append(".patient SET ");
        StringBuilder updates = new StringBuilder();
        
        // Build dynamic UPDATE statement based on payload
        for (Map.Entry<String, Object> entry : changes.entrySet()) {
            String column = mapPortalFieldToEhrColumn(entry.getKey());
            if (column != null) {
                if (updates.length() > 0) updates.append(", ");
                updates.append(column).append(" = ?");
            }
        }
        
        if (updates.length() == 0) {
            log.warn("⚠️ No valid demographics fields to update");
            return;
        }
        
        sql.append(updates).append(", last_modified_date = NOW() WHERE portal_user_id = ?");
        
        // Prepare parameters
        Object[] params = new Object[changes.size() + 1];
        int i = 0;
        for (Map.Entry<String, Object> entry : changes.entrySet()) {
            String column = mapPortalFieldToEhrColumn(entry.getKey());
            if (column != null) {
                params[i++] = entry.getValue();
            }
        }
        params[i] = update.getUserId();
        
        int rowsUpdated = jdbcTemplate.update(sql.toString(), params);
        log.info("📝 Demographics updated - Rows affected: {}", rowsUpdated);
    }

    /**
     * Merge insurance data into tenant insurance table
     */
    private void mergeInsuranceData(String tenantSchema, PortalPendingUpdate update) {
        Map<String, Object> changes = update.getPayload();
        
        // Check if insurance record exists
        String checkSql = "SELECT COUNT(*) FROM " + tenantSchema + ".insurance WHERE patient_id = " +
                         "(SELECT id FROM " + tenantSchema + ".patient WHERE portal_user_id = ?)";
        
        int count = jdbcTemplate.queryForObject(checkSql, Integer.class, update.getUserId());
        
        if (count > 0) {
            // Update existing insurance
            updateInsuranceRecord(tenantSchema, update, changes);
        } else {
            // Insert new insurance record
            insertInsuranceRecord(tenantSchema, update, changes);
        }
    }

    /**
     * Merge billing data into tenant billing table
     */
    private void mergeBillingData(String tenantSchema, PortalPendingUpdate update) {
        Map<String, Object> changes = update.getPayload();
        
        // Insert billing update/note
        String sql = "INSERT INTO " + tenantSchema + ".billing_notes " +
                    "(patient_id, note_type, notes, created_date, created_by) VALUES " +
                    "((SELECT id FROM " + tenantSchema + ".patient WHERE portal_user_id = ?), 'PORTAL_UPDATE', ?, NOW(), 'PORTAL_SYSTEM')";
        
        String notes = "Patient portal billing update: " + changes.toString();
        jdbcTemplate.update(sql, update.getUserId(), notes);
        
        log.info("💰 Billing note added for patient");
    }

    /**
     * Merge emergency contact data
     */
    private void mergeEmergencyContactData(String tenantSchema, PortalPendingUpdate update) {
        Map<String, Object> changes = update.getPayload();
        
        // Update or insert emergency contact
        String sql = "INSERT INTO " + tenantSchema + ".emergency_contacts " +
                    "(patient_id, contact_name, phone, relationship, created_date) VALUES " +
                    "((SELECT id FROM " + tenantSchema + ".patient WHERE portal_user_id = ?), ?, ?, ?, NOW()) " +
                    "ON CONFLICT (patient_id) DO UPDATE SET " +
                    "contact_name = EXCLUDED.contact_name, phone = EXCLUDED.phone, relationship = EXCLUDED.relationship";
        
        jdbcTemplate.update(sql, 
                update.getUserId(),
                changes.get("contactName"),
                changes.get("phone"),
                changes.get("relationship"));
        
        log.info("🚨 Emergency contact updated");
    }

    /**
     * Merge messaging data into communication table
     */
    private void mergeMessagingData(String tenantSchema, PortalPendingUpdate update) {
        Map<String, Object> changes = update.getPayload();
        
        String sql = "INSERT INTO " + tenantSchema + ".communication " +
                    "(patient_id, message_type, message, status, created_date, created_by) VALUES " +
                    "((SELECT id FROM " + tenantSchema + ".patient WHERE portal_user_id = ?), 'PORTAL_MESSAGE', ?, 'UNREAD', NOW(), 'PATIENT_PORTAL')";
        
        jdbcTemplate.update(sql, update.getUserId(), changes.get("message"));
        log.info("💬 Patient message added to communication log");
    }

    /**
     * Helper methods for insurance operations
     */
    private void updateInsuranceRecord(String tenantSchema, PortalPendingUpdate update, Map<String, Object> changes) {
        String sql = "UPDATE " + tenantSchema + ".insurance SET " +
                    "insurance_company = ?, policy_number = ?, group_number = ?, last_modified_date = NOW() " +
                    "WHERE patient_id = (SELECT id FROM " + tenantSchema + ".patient WHERE portal_user_id = ?)";
        
        jdbcTemplate.update(sql,
                changes.get("insuranceCompany"),
                changes.get("policyNumber"),
                changes.get("groupNumber"),
                update.getUserId());
        
        log.info("🏥 Insurance record updated");
    }

    private void insertInsuranceRecord(String tenantSchema, PortalPendingUpdate update, Map<String, Object> changes) {
        String sql = "INSERT INTO " + tenantSchema + ".insurance " +
                    "(patient_id, insurance_company, policy_number, group_number, created_date) VALUES " +
                    "((SELECT id FROM " + tenantSchema + ".patient WHERE portal_user_id = ?), ?, ?, ?, NOW())";
        
        jdbcTemplate.update(sql,
                update.getUserId(),
                changes.get("insuranceCompany"),
                changes.get("policyNumber"),
                changes.get("groupNumber"));
        
        log.info("🏥 New insurance record created");
    }

    /**
     * Map portal field names to EHR column names
     */
    private String mapPortalFieldToEhrColumn(String portalField) {
        switch (portalField) {
            case "firstName": return "first_name";
            case "lastName": return "last_name";
            case "dateOfBirth": return "date_of_birth";
            case "addressLine1": return "address_line1";
            case "addressLine2": return "address_line2";
            case "city": return "city";
            case "state": return "state";
            case "postalCode": return "postal_code";
            case "phoneNumber": return "phone_number";
            case "email": return "email";
            default:
                log.warn("⚠️ Unknown portal field: {}", portalField);
                return null;
        }
    }
}