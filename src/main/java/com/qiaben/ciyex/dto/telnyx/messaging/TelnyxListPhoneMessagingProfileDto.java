package com.qiaben.ciyex.dto.telnyx.messaging;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxListPhoneMessagingProfileDto {
    private List<PhoneNumber> data;
    private Meta meta;

    @Data
    public static class PhoneNumber {
        private String recordType;
        private String id;
        private String phoneNumber;
        private String messagingProfileId;
        private String createdAt;
        private String updatedAt;
        private String countryCode;
        private String type;
        private Health health;
        private Features features;
    }

    @Data
    public static class Health {
        private int messageCount;
        private float inboundOutboundRatio;
        private float successRatio;
        private float spamRatio;
    }

    @Data
    public static class Features {
        private Sms sms;
        private Mms mms;
    }

    @Data
    public static class Sms {
        private boolean domesticTwoWay;
        private boolean internationalInbound;
        private boolean internationalOutbound;
    }

    @Data
    public static class Mms {
        private boolean domesticTwoWay;
        private boolean internationalInbound;
        private boolean internationalOutbound;
    }

    @Data
    public static class Meta {
        private int totalPages;
        private int totalResults;
        private int pageNumber;
        private int pageSize;
    }
}
