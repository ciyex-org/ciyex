package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.MuteConferenceParticipantsRequestDto;
import com.qiaben.ciyex.dto.telnyx.MuteConferenceParticipantsResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class MuteConferenceParticipantsService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient = RestClient.create();

    public MuteConferenceParticipantsResponseDto muteParticipants(String conferenceId, MuteConferenceParticipantsRequestDto request) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/conferences/" + conferenceId + "/actions/mute";

        return restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(MuteConferenceParticipantsResponseDto.class);
    }
}
