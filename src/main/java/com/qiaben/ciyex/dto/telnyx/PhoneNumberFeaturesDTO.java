
// src/main/java/com/qiaben/ciyex/dto/telnyx/PhoneNumberFeaturesDTO.java
package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class PhoneNumberFeaturesDTO {

    @Data
    public static class Sms {
        private Boolean domesticTwoWay;
        private Boolean internationalInbound;
        private Boolean internationalOutbound;
    }

    @Data
    public static class Mms {
        private Boolean domesticTwoWay;
        private Boolean internationalInbound;
        private Boolean internationalOutbound;
    }

    private Sms sms;
    private Mms mms;
}

