package org.ciyex.ehr.dto.telnyx.messaging;

import lombok.Data;
import java.util.List;

@Data
public class TelnyxShortCodesDto {
    private List<ShortCodeData> data;
    private Meta meta;

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

    @Data
    public static class Meta {
        private int total_pages;
        private int total_results;
        private int page_number;
        private int page_size;
    }
}

