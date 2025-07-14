package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class PushCredentialsResponseDto {
    private String id;
    private String certificate;
    private String private_key;
    private Map<String, Object> project_account_json_file;
    private String alias;
    private String type;
    private String record_type;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
}
