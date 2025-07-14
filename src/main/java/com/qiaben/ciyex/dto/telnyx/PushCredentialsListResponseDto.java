package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class PushCredentialsListResponseDto {
    private List<PushCredentialDto> data;
    private Meta meta;

    @Data
    public static class PushCredentialDto {
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

    @Data
    public static class Meta {
        private int page_number;
        private int page_size;
        private int total_pages;
        private int total_results;
    }
}
