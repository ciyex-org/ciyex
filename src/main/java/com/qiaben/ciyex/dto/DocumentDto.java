package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class DocumentDto {
    private Long id;
    private Long patientId;
    private String category;
    private String type;
    private String fileName;
    private String contentType;
    private String description;

    private byte[] content; // transient, only for upload
    private String s3Bucket; // transient
    private String s3Key;    // transient

    // NEW
    private boolean encrypted;
}
