package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;
import java.util.Map;

@Data
public class TelnyxDialogflowRequestDTO {
    private String connectionId;
    private Map<String, Object> serviceAccount;
    private String dialogflowApi = "es"; // Default
    private String conversationProfileId;
    private String location;
    private String environment;
}
