package com.qiaben.ciyex.dto.fhir;

import lombok.Data;

@Data
public class FhirBinaryResponseDTO {
    private String id;
    private String resourceType;
    private byte[] content; // Assuming binary data returned in byte array
    private String mimeType;
}

