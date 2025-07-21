
// src/main/java/com/qiaben/ciyex/dto/telnyx/PhoneNumberHealthDTO.java
package com.qiaben.ciyex.dto.telnyx.messaging;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TelnyxPhoneNumberHealthDTO {
    private Integer messageCount;
    private BigDecimal inboundOutboundRatio;
    private BigDecimal successRatio;
    private BigDecimal spamRatio;
}

