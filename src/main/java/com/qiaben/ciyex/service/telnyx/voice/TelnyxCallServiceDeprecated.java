package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxCallResponseDtoDeprecated;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxInitiateCallRequestDtoDeprecated;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxUpdateCallRequestDtoDeprecated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxCallServiceDeprecated {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient;

    public TelnyxCallResponseDtoDeprecated initiateCall(String applicationId, TelnyxInitiateCallRequestDtoDeprecated request) {
        return restClient.post()
                .uri(telnyxProperties.getApiBaseUrl() + "/v2/texml/calls/{applicationId}", applicationId)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .body(request)
                .retrieve()
                .body(TelnyxCallResponseDtoDeprecated.class);
    }

    public TelnyxCallResponseDtoDeprecated updateCall(String callSid, TelnyxUpdateCallRequestDtoDeprecated request) {
        return restClient.post()
                .uri(telnyxProperties.getApiBaseUrl() + "/v2/texml/calls/{callSid}/update", callSid)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .body(request)
                .retrieve()
                .body(TelnyxCallResponseDtoDeprecated.class);
    }
}
