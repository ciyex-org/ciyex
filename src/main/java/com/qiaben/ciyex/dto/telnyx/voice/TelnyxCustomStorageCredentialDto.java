package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;
import java.util.Map;

@Data
public class TelnyxCustomStorageCredentialDto {
    private String connectionId;
    private String backend; // gcs | s3 | azure
    private Map<String, Object> configuration; // provider‑specific fields
    private String recordType; // custom_storage_credentials
}