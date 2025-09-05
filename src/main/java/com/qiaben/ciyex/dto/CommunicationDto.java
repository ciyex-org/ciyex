package com.qiaben.ciyex.dto;

import com.qiaben.ciyex.entity.CommunicationStatus;
import lombok.Data;

import java.util.List;

@Data
public class CommunicationDto {
    private Long id;
    private String externalId;
    private Long orgId;

    private CommunicationStatus status;
    private String category;
    private String sentDate;
    private String createdDate;
    private String lastModifiedDate;
    private String payload;
    private String sender;
    private List<String> recipients;
    private String subject;
    private String inResponseTo;

    private Long patientId;
    private Long providerId;

    private Long fromId;
    private String fromName;
    private List<Long> toIds;
    private List<String> toNames;
}

