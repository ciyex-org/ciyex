package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxConferenceResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxConferenceRetrieveService {

    private final RestClient restClient;
    private final TelnyxProperties telnyxProperties;

    public TelnyxConferenceResponseDto getConferenceById(String id) {
        String uri = telnyxProperties.getApiBaseUrl() + "/v2/conferences/" + id;

        return restClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxConferenceResponseDto.class);
    }
}
