
// src/main/java/com/ciyex/ciyex/dto/telnyx/PhoneNumberHealthDTO.java
package org.ciyex.ehr.dto.telnyx.messaging;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TelnyxPhoneNumberHealthDTO {
    private Integer messageCount;
    private BigDecimal inboundOutboundRatio;
    private BigDecimal successRatio;
    private BigDecimal spamRatio;
}

