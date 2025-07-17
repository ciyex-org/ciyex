// src/main/java/com/qiaben/ciyex/dto/telnyx/UploadRequestDetailResponseDTO.java
package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxUploadRequestDetailResponseDTO {
    private Data data;

    @lombok.Data
    public static class Data {
        private String ticket_id;
        private String tenant_id;
        private String location_id;
        private String status;
        private List<String> available_usages;
        private String error_code;
        private String error_message;
        private List<TnUploadEntry> tn_upload_entries;
    }

    @lombok.Data
    public static class TnUploadEntry {
        private String number_id;
        private String phone_number;
        private String status;
        private String error_code;
        private String error_message;
        private String civic_address_id;
        private String location_id;
        private String internal_status;
    }
}
