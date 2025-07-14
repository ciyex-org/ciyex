package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.SiprecSessionResponseDto;
import com.qiaben.ciyex.dto.telnyx.StartSiprecSessionRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class SiprecSessionService {

    private final RestClient restClient;
    private final TelnyxProperties telnyxProperties;

    public SiprecSessionResponseDto startSiprecSession(String accountSid, String callSid, StartSiprecSessionRequestDto dto) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        if (dto.getConnectorName() != null) body.add("ConnectorName", dto.getConnectorName());
        if (dto.getName() != null) body.add("Name", dto.getName());
        if (dto.getTrack() != null) body.add("Track", dto.getTrack());
        if (dto.getIncludeMetadataCustomHeaders() != null) body.add("IncludeMetadataCustomHeaders", dto.getIncludeMetadataCustomHeaders().toString());
        if (dto.getSecure() != null) body.add("Secure", dto.getSecure().toString());
        if (dto.getSessionTimeoutSecs() != null) body.add("SessionTimeoutSecs", dto.getSessionTimeoutSecs().toString());
        if (dto.getSipTransport() != null) body.add("SipTransport", dto.getSipTransport());
        if (dto.getStatusCallback() != null) body.add("StatusCallback", dto.getStatusCallback());
        if (dto.getStatusCallbackMethod() != null) body.add("StatusCallbackMethod", dto.getStatusCallbackMethod());

        return restClient.post()
                .uri(telnyxProperties.getApiBaseUrl() + "/v2/texml/Accounts/{accountSid}/Calls/{callSid}/SiprecSessions", accountSid, callSid)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(body)
                .retrieve()
                .body(SiprecSessionResponseDto.class);
    }
}
