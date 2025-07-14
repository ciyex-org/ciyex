package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.ActiveCallListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.HttpHeaders;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ActiveCallService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient = RestClient.create();

    public ActiveCallListResponseDto getActiveCalls(String connectionId, Map<String, String> queryParams) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(telnyxProperties.getApiBaseUrl() + "/v2/connections/" + connectionId + "/active_calls");

        queryParams.forEach(uriBuilder::queryParam);

        return restClient.get()
                .uri(uriBuilder.toUriString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(ActiveCallListResponseDto.class);
    }
}
