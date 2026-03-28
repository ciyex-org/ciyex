package org.ciyex.ehr.dto.portal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for patient update requests (demographics, insurance, billing, etc.)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalUpdateRequest {
    
    /**
     * Type of update being submitted
     * DEMOGRAPHICS, INSURANCE, BILLING, MESSAGING, APPOINTMENT, EMERGENCY_CONTACT
     */
    private String updateType;
    
    /**
     * JSON payload containing the actual changes
     * Example: {"addressLine1": "123 Main St", "city": "Dallas", "state": "TX"}
     */
    private Map<String, Object> changes;
    
    /**
     * Optional hint/description for EHR staff
     * Example: "Updated address due to recent move"
     */
    private String hint;
    
    /**
     * Priority level: LOW, NORMAL, HIGH, URGENT
     */
    private String priority;
    
    /**
     * Patient notes for the update
     */
    private String patientNotes;
}