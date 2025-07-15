package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.ConferenceRecordingPauseRequestDto;
import com.qiaben.ciyex.dto.telnyx.ConferenceRecordingPauseResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class ConferenceRecordingPauseService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient = RestClient.create();

    public ConferenceRecordingPauseResponseDto pauseRecording(String conferenceId, ConferenceRecordingPauseRequestDto request) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/conferences/" + conferenceId + "/actions/record_pause";

        return restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ConferenceRecordingPauseResponseDto.class);
    }
}
