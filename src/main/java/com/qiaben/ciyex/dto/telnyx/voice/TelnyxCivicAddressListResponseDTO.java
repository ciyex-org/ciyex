package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;
import java.util.List;

@Data
public class TelnyxCivicAddressListResponseDTO {
    private List<CivicAddress> data;

    @Data
    public static class CivicAddress {
        private String id;
        private String record_type;
        private String city_or_town;
        private String city_or_town_alias;
        private String company_name;
        private String country;
        private String country_or_district;
        private String default_location_id;
        private String description;
        private String house_number;
        private String house_number_suffix;
        private String postal_or_zip_code;
        private String state_or_province;
        private String street_name;
        private String street_suffix;
        private List<Location> locations;
    }

    @Data
    public static class Location {
        private String id;
        private String additional_info;
        private String description;
        private boolean is_default;

        // Fields for static emergency address update response
        private String static_emergency_address_id;
        private boolean accepted_address_suggestions;
    }
}
