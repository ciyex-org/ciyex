package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxMuteConferenceParticipantsRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxMuteConferenceParticipantsResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxMuteConferenceParticipantsService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient = RestClient.create();

    public TelnyxMuteConferenceParticipantsResponseDto muteParticipants(String conferenceId, TelnyxMuteConferenceParticipantsRequestDto request) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/conferences/" + conferenceId + "/actions/mute";

        return restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(TelnyxMuteConferenceParticipantsResponseDto.class);
    }
}
