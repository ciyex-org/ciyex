package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;
import java.util.List;

@Data
public class TelnyxCancelScheduleMessageDto {
    private String recordType;
    private String direction;
    private String id;
    private String type;
    private String messagingProfileId;
    private String organizationId;
    private From from;
    private List<To> to;
    private String text;
    private String subject;
    private List<Media> media;
    private String webhookUrl;
    private String webhookFailoverUrl;
    private String encoding;
    private Integer parts;
    private List<String> tags;
    private Cost cost;
    private CostBreakdown costBreakdown;
    private CarrierFee carrierFee;
    private Rate rate;
    private String tcrCampaignId;
    private Boolean tcrCampaignBillable;
    private String tcrCampaignRegistered;
    private String receivedAt;
    private String sentAt;
    private String completedAt;
    private String validUntil;
    private List<Error> errors;

    @Data
    public static class From {
        private String phoneNumber;
        private String carrier;
        private String lineType;
    }

    @Data
    public static class To {
        private String phoneNumber;
        private String status;
        private String carrier;
        private String lineType;
    }

    @Data
    public static class Media {
        private String url;
        private String contentType;
        private String sha256;
        private Integer size;
    }

    @Data
    public static class Cost {
        private String amount;
        private String currency;
    }

    @Data
    public static class CostBreakdown {
        // Define fields as required
    }

    @Data
    public static class CarrierFee {
        private String amount;
        private String currency;
    }

    @Data
    public static class Rate {
        private String amount;
        private String currency;
    }

    @Data
    public static class Error {
        private Integer code;
        private String title;
        private String detail;
        private Source source;
        private Object meta;
    }

    @Data
    public static class Source {
        private String pointer;
        private String parameter;
    }
}
