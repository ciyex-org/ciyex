package org.ciyex.ehr.fax.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Entity @Table(name = "fax_message")
@Builder @NoArgsConstructor @AllArgsConstructor
public class FaxMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String direction;       // inbound, outbound
    private String faxNumber;
    private String senderName;
    private String recipientName;
    private String subject;
    private Integer pageCount;
    private String status;          // pending, sending, sent, delivered, failed, received, categorized, attached
    private Long patientId;
    private String patientName;
    private String category;        // referral, lab_result, prior_auth, medical_records, other
    private String documentUrl;
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    private LocalDateTime sentAt;
    private LocalDateTime receivedAt;
    private String processedBy;
    private LocalDateTime processedAt;
    @Column(columnDefinition = "TEXT")
    private String notes;
    private String orgAlias;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}
