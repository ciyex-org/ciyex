package com.qiaben.ciyex.dto.telnyx.messaging;

import lombok.Data;

// DTO for PATCH/update short code response and for GET /v2/short_codes/{id}
@Data
public class TelnyxSingleShortCodeDto {
    private ShortCodeData data;

    @Data
    public static class ShortCodeData {
        private String record_type;
        private String id;
        private String short_code;
        private String country_code;
        private String messaging_profile_id;
        private String created_at;
        private String updated_at;
    }
}
