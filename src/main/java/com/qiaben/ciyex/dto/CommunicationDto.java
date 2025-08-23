package com.qiaben.ciyex.dto;

import lombok.Data;

import java.util.List;

@Data
public class CommunicationDto {
    private Long id;
    private String externalId;
    private Long orgId;
    private String status;
    private String category;
    private String sentDate;
    private Audit audit;
    private String payload;
    private String sender; // Restored as string with externalId reference
    private List<String> recipients; // Restored as list of strings with externalId references
    private String subject;
    private String inResponseTo;
    private Long patientId;
    private Long providerId;
    private Long fromId; // For response, internal ID of sender
    private String fromName; // For response, name of sender
    private List<Long> toIds; // For response, internal IDs of recipients
    private List<String> toNames; // For response, names of recipients

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}