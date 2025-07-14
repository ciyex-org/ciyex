package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;
import java.util.List;

@Data
public class PhoneNumberListResponseDTO {
    private List<PhoneNumber> data;
    private Meta meta;

    @Data
    public static class PhoneNumber {
        private String ticket_id;
        private String telephone_number;
        private String number_id;
        private String civic_address_id;
        private String location_id;
        private String displayed_country_code;
        private List<String> acquired_capabilities;
    }

    @Data
    public static class Meta {
        private int total_pages;
        private int total_results;
        private int page_number;
        private int page_size;
    }
}
