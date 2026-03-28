package org.ciyex.ehr.fax.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FaxMessageDto {
    private Long id;
    private String direction;
    private String faxNumber;
    private String senderName;
    private String recipientName;
    private String subject;
    private Integer pageCount;
    private String status;
    private Long patientId;
    private String patientName;
    private String category;
    private String documentUrl;
    private String errorMessage;
    private String sentAt;
    private String receivedAt;
    private String processedBy;
    private String processedAt;
    private String notes;
    private String createdAt;
    private String updatedAt;
}
