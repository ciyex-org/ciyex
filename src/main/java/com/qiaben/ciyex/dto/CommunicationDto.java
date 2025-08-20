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

    private String sender;
    private List<String> recipients;
    private String subject;
    private String inResponseTo;

    private Long patientId; // Added for request body
    private Long providerId; // Added for request body

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}