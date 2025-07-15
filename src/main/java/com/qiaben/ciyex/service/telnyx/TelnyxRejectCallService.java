package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.TelnyxRejectCallRequestDTO;
import com.qiaben.ciyex.dto.telnyx.TelnyxRejectCallResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxRejectCallService {

    private final TelnyxProperties telnyxProperties;

    public TelnyxRejectCallResponseDTO rejectCall(String callControlId, TelnyxRejectCallRequestDTO request) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/calls/" + callControlId + "/actions/reject";

        RestClient restClient = RestClient.builder()
                .baseUrl(url)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        return restClient.post()
                .body(request)
                .retrieve()
                .body(TelnyxRejectCallResponseDTO.class);
    }
}
