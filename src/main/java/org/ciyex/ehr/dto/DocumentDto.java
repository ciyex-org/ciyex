package org.ciyex.ehr.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentDto {
    private Long id;
    private String fhirId;  // Actual FHIR DocumentReference ID
    private Long patientId;
    private String title;
    private String category;
    private String status;
    private String type;
    private String fileName;
    private String contentType;
    private String description;
    @JsonAlias("date")
    private String documentDate;
    private String author;

    private byte[] content; // transient, only for upload
    private String s3Key;   // transient, storage key for Vaultik

    private boolean encrypted;
}
