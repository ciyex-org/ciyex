
// src/main/java/com/ciyex/ciyex/dto/telnyx/MessagingPhoneNumberDTO.java
package org.ciyex.ehr.dto.telnyx.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class TelnyxMessagingPhoneNumberDTO {
    private String recordType;
    private String id;
    @JsonProperty("phone_number")
    private String phoneNumber;
    private String messagingProfileId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String countryCode;
    private String type;
    private TelnyxPhoneNumberHealthDTO health;
    private List<String> eligibleMessagingProducts;
    private String trafficType;
    private String messagingProduct;
    private TelnyxPhoneNumberFeaturesDTO features;
}
