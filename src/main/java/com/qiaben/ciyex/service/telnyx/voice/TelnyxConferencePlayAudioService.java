package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxConferencePlayAudioRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxConferencePlayAudioResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxConferencePlayAudioService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient = RestClient.create();

    public TelnyxConferencePlayAudioResponseDto playAudio(String conferenceId, TelnyxConferencePlayAudioRequestDto request) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/conferences/" + conferenceId + "/actions/play";

        return restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(TelnyxConferencePlayAudioResponseDto.class);
    }
}
