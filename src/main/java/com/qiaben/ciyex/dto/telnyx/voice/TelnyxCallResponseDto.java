package com.qiaben.ciyex.dto.telnyx.voice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TelnyxCallResponseDto {
    @JsonProperty("account_sid")
    private String accountSid;

    @JsonProperty("answered_by")
    private String answeredBy;

    @JsonProperty("caller_name")
    private String callerName;

    @JsonProperty("date_created")
    private String dateCreated;

    @JsonProperty("date_updated")
    private String dateUpdated;

    private String direction;
    private String duration;

    @JsonProperty("end_time")
    private String endTime;

    private String from;

    @JsonProperty("from_formatted")
    private String fromFormatted;

    private String price;

    @JsonProperty("price_unit")
    private String priceUnit;

    private String sid;

    @JsonProperty("start_time")
    private String startTime;

    private String status;
    private String to;

    @JsonProperty("to_formatted")
    private String toFormatted;

    private String uri;
}

