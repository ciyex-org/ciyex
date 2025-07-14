package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;
import java.util.Map;

@Data
public class DialogflowRequestDTO {
    private String connectionId;
    private Map<String, Object> serviceAccount;
    private String dialogflowApi = "es"; // Default
    private String conversationProfileId;
    private String location;
    private String environment;
}
