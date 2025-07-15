package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;
import java.util.List;

@Data
public class TelnyxReferCallRequestDTO {
    private String sipAddress;
    private String clientState;
    private String commandId;
    private List<CustomHeader> customHeaders;
    private String sipAuthUsername;
    private String sipAuthPassword;
    private List<SipHeader> sipHeaders;

    @Data
    public static class CustomHeader {
        private String name;
        private String value;
    }

    @Data
    public static class SipHeader {
        private String name;
        private String value;
    }
}
