package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.CallStatusResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.HttpHeaders;

@Service
@RequiredArgsConstructor
public class CallStatusService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient = RestClient.create();

    public CallStatusResponseDto getCallStatus(String callControlId) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/calls/" + callControlId;

        return restClient.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(CallStatusResponseDto.class);
    }
}
