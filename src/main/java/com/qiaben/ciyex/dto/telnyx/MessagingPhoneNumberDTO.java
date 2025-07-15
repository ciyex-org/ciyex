package com.qiaben.ciyex.dto.telnyx;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class MessagingPhoneNumberDTO {
    private String recordType;
    private String id;
    @JsonProperty("phone_number")
    private String phoneNumber;
    private String messagingProfileId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String countryCode;
    private String type;
    private PhoneNumberHealthDTO health;
    private List<String> eligibleMessagingProducts;
    private String trafficType;
    private String messagingProduct;
    private PhoneNumberFeaturesDTO features;
}
