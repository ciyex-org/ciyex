package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxConferenceCreateRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxConferenceResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxConferenceCreateService {

    private final RestClient restClient;
    private final TelnyxProperties telnyxProperties;

    public TelnyxConferenceResponseDto createConference(TelnyxConferenceCreateRequestDto req) {

        return restClient
                .post()
                .uri(telnyxProperties.getApiBaseUrl() + "/v2/conferences")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(req)
                .retrieve()
                .body(TelnyxConferenceResponseDto.class);
    }
}

