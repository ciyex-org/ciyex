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
    private byte[] content; // Transient: For create (file bytes)
    private String s3Bucket; // Transient: For download
    private String s3Key; // Transient: For download
}