package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxReleaseListResponseDTO {
    private List<Release> data;
    private Meta meta;

    @Data
    public static class Release {
        private String ticket_id;
        private String tenant_id;
        private String status;
        private String error_message;
        private List<TelephoneNumber> telephone_numbers;
        private String created_at;
    }

    @Data
    public static class TelephoneNumber {
        private String phone_number;
        private String number_id;
    }

    @Data
    public static class Meta {
        private Integer total_pages;
        private Integer total_results;
        private Integer page_number;
        private Integer page_size;
    }
}
