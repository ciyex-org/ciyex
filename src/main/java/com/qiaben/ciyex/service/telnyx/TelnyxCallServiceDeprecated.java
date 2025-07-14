package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.CallResponseDtoDeprecated;
import com.qiaben.ciyex.dto.telnyx.InitiateCallRequestDtoDeprecated;
import com.qiaben.ciyex.dto.telnyx.UpdateCallRequestDtoDeprecated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxCallServiceDeprecated {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient;

    public CallResponseDtoDeprecated initiateCall(String applicationId, InitiateCallRequestDtoDeprecated request) {
        return restClient.post()
                .uri(telnyxProperties.getApiBaseUrl() + "/v2/texml/calls/{applicationId}", applicationId)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .body(request)
                .retrieve()
                .body(CallResponseDtoDeprecated.class);
    }

    public CallResponseDtoDeprecated updateCall(String callSid, UpdateCallRequestDtoDeprecated request) {
        return restClient.post()
                .uri(telnyxProperties.getApiBaseUrl() + "/v2/texml/calls/{callSid}/update", callSid)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .body(request)
                .retrieve()
                .body(CallResponseDtoDeprecated.class);
    }
}
