package com.qiaben.ciyex.entity.portal;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity for storing patient updates that require EHR staff review
 * This acts as a review queue between Portal and EHR systems
 */
@Entity
@Table(name = "portal_pending_updates", schema = "public")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PortalPendingUpdate extends com.qiaben.ciyex.entity.AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "update_type", nullable = false, length = 50)
    private String updateType; // DEMOGRAPHICS, INSURANCE, BILLING, MESSAGING, APPOINTMENT

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "hint", length = 500)
    private String hint; // Description for EHR staff

    @Column(name = "priority", length = 20)
    @Builder.Default
    private String priority = "NORMAL"; // LOW, NORMAL, HIGH, URGENT

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED

    @Column(name = "patient_notes", length = 1000)
    private String patientNotes;

    @Column(name = "approver_notes", length = 1000)
    private String approverNotes;

    @Column(name = "approved_by", length = 255)
    private String approvedBy; // Email of EHR staff who approved/rejected

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    // audit fields provided by AuditableEntity

    @Column(name = "reviewed_date")
    private LocalDateTime reviewedDate;

    // Helper methods
    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public boolean isApproved() {
        return "APPROVED".equals(status);
    }

    public boolean isRejected() {
        return "REJECTED".equals(status);
    }

    public void approve(String approverEmail, String notes) {
        this.status = "APPROVED";
        this.approvedBy = approverEmail;
        this.approverNotes = notes;
    this.reviewedDate = LocalDateTime.now();
    setLastModifiedDate(LocalDateTime.now());
    }

    public void reject(String approverEmail, String reason, String notes) {
        this.status = "REJECTED";
        this.approvedBy = approverEmail;
        this.rejectionReason = reason;
        this.approverNotes = notes;
    this.reviewedDate = LocalDateTime.now();
    setLastModifiedDate(LocalDateTime.now());
    }
}