package com.qiaben.ciyex.dto.telnyx.messaging;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class TelnyxRetrieveMessageDto {
    private DataObj data;

    @Data
    public static class DataObj {
        private String record_type;
        private String direction;
        private String id;
        private String type;
        private String messaging_profile_id;
        private String organization_id;
        private FromObj from;
        private List<ToObj> to;
        private String text;
        private String subject;
        private List<MediaObj> media;
        private String webhook_url;
        private String webhook_failover_url;
        private String encoding;
        private Integer parts;
        private List<String> tags;
        private CostObj cost;
        private Object cost_breakdown; // Expand if needed
        private CarrierFeeObj carrier_fee;
        private RateObj rate;
        private String tcr_campaign_id;
        private Boolean tcr_campaign_billable;
        private String tcr_campaign_registered;
        private String received_at;
        private String sent_at;
        private String completed_at;
        private String valid_until;
        private List<ErrorObj> errors;
    }

    @Data
    public static class FromObj {
        private String phone_number;
        private String carrier;
        private String line_type;
    }

    @Data
    public static class ToObj {
        private String phone_number;
        private String status;
        private String carrier;
        private String line_type;
    }

    @Data
    public static class MediaObj {
        private String url;
        private String content_type;
        private String sha256;
        private Integer size;
    }

    @Data
    public static class CostObj {
        private BigDecimal amount;
        private String currency;
    }

    @Data
    public static class CarrierFeeObj {
        private BigDecimal amount;
        private String currency;
    }

    @Data
    public static class RateObj {
        private BigDecimal amount;
        private String currency;
    }

    @Data
    public static class ErrorObj {
        private Integer code;
        private String title;
        private String detail;
        private ErrorSourceObj source;
        private Object meta;
    }

    @Data
    public static class ErrorSourceObj {
        private String pointer;
        private String parameter;
    }
}
