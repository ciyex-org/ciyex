package org.ciyex.ehr.dto.portal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for displaying pending updates to both patients and EHR staff
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalPendingUpdateDto {
    
    private Long id;
    private UUID userId;
    private String patientName;
    private String patientEmail;
    private String updateType;
    private Map<String, Object> payload;
    private String hint;
    private String priority;
    private String status; // PENDING, APPROVED, REJECTED
    private String patientNotes;
    private String approverNotes;
    private String approvedBy;
    private String rejectionReason;
    private LocalDateTime createdDate;
    private LocalDateTime reviewedDate;
    
    /**
     * For patient view - simplified status message
     */
    public String getStatusMessage() {
        switch (status) {
            case "PENDING":
                return "Under Review";
            case "APPROVED":
                return "Approved - Changes Applied";
            case "REJECTED":
                return "Rejected - " + (rejectionReason != null ? rejectionReason : "See notes");
            default:
                return status;
        }
    }
    
    /**
     * Get formatted update type for display
     */
    public String getUpdateTypeDisplay() {
        switch (updateType) {
            case "DEMOGRAPHICS":
                return "Personal Information";
            case "INSURANCE":
                return "Insurance Information";
            case "BILLING":
                return "Billing & Payment";
            case "MESSAGING":
                return "Message to Clinic";
            case "EMERGENCY_CONTACT":
                return "Emergency Contact";
            case "APPOINTMENT":
                return "Appointment Request";
            default:
                return updateType;
        }
    }
}