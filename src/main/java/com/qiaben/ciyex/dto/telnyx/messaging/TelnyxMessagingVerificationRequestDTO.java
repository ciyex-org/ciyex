package com.qiaben.ciyex.dto.telnyx.messaging;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TelnyxMessagingVerificationRequestDTO {
    private List<VerificationRecord> records;
    private int total_records;

    @Data
    public static class VerificationRecord {
        private String businessName;
        private String corporateWebsite;
        private String businessAddr1;
        private String businessAddr2;
        private String businessCity;
        private String businessState;
        private String businessZip;
        private String businessContactFirstName;
        private String businessContactLastName;
        private String businessContactEmail;
        private String businessContactPhone;
        private String messageVolume;
        private List<String> phoneNumbers;
        private String useCase;
        private String useCaseSummary;
        private String productionMessageContent;
        private String optInWorkflow;
        private List<String> optInWorkflowImageURLs;
        private String additionalInformation;
        private String isvReseller;
        private String webhookUrl;
        private String id;
        private String verificationStatus;
        private String reason;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
