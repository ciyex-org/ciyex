package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxConferenceSpeakRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxConferenceSpeakResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxConferenceSpeakService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient = RestClient.create();

    public TelnyxConferenceSpeakResponseDto speak(String conferenceId, TelnyxConferenceSpeakRequestDto request) {
        String url = telnyxProperties.getApiBaseUrl() +
                "/v2/conferences/" + conferenceId + "/actions/speak";

        return restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(TelnyxConferenceSpeakResponseDto.class);
    }
}
