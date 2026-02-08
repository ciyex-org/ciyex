package org.ciyex.ehr.dto.telnyx.messaging;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxListShortcodesMessagingProfileDto {

    private List<Shortcode> data;
    private Meta meta;

    @Data
    public static class Shortcode {
        private String recordType;
        private String id;
        private String shortCode;
        private String countryCode;
        private String messagingProfileId;
        private String createdAt;
        private String updatedAt;
    }

    @Data
    public static class Meta {
        private int totalPages;
        private int totalResults;
        private int pageNumber;
        private int pageSize;
    }
}
