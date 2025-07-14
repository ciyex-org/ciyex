package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.ConferenceRecordingResumeRequestDto;
import com.qiaben.ciyex.dto.telnyx.ConferenceRecordingResumeResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class ConferenceRecordingResumeService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient = RestClient.create();

    public ConferenceRecordingResumeResponseDto resumeRecording(String conferenceId, ConferenceRecordingResumeRequestDto request) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/conferences/" + conferenceId + "/actions/record_resume";

        return restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ConferenceRecordingResumeResponseDto.class);
    }
}
