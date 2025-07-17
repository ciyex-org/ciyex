// src/main/java/com/qiaben/ciyex/dto/telnyx/UploadListResponseDTO.java
package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxUploadListResponseDTO {
    private List<Upload> data;
    private Meta meta;

    @Data
    public static class Upload {
        private String ticket_id;
        private String tenant_id;
        private String location_id;
        private String status; // pending_upload, pending, in_progress, success, error
        private List<String> available_usages;
        private String error_code;
        private String error_message;
        private List<TnUploadEntry> tn_upload_entries;
    }

    @Data
    public static class TnUploadEntry {
        private String number;
        private String status;
        private String usage;
        private String error_message;
    }

    @Data
    public static class Meta {
        private Integer total_pages;
        private Integer total_results;
        private Integer page_number;
        private Integer page_size;
    }
}
