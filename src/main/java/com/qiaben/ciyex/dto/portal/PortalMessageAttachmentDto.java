package com.qiaben.ciyex.dto.portal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalMessageAttachmentDto {
    private Long id;
    private Long messageId;
    private String category;
    private String type;
    private String fileName;
    private String contentType;
    private String description;
    private String createdDate;
    private String lastModifiedDate;
    private boolean encrypted;

    // Portal-specific fields
    private String downloadUrl; // Portal-specific download URL
    private String fileSize; // Human-readable file size
    private String uploadedBy; // Who uploaded (patient/provider)
}