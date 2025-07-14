package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;
import java.util.Map;

@Data
public class CustomStorageCredentialDto {
    private String connectionId;
    private String backend; // gcs | s3 | azure
    private Map<String, Object> configuration; // provider‑specific fields
    private String recordType; // custom_storage_credentials
}