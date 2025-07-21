package com.qiaben.ciyex.service.telnyx.video;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxRecordingCommandRequestDTO;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxRecordingCommandResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxRecordingService {

    private final TelnyxProperties properties;

    private RestClient getClient() {
        return RestClient.builder()
                .baseUrl(properties.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .build();
    }

    public TelnyxRecordingCommandResponseDTO startRecording(String callControlId, TelnyxRecordingCommandRequestDTO body) {
        return getClient()
                .post()
                .uri("/v2/calls/{id}/actions/record_start", callControlId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(TelnyxRecordingCommandResponseDTO.class);
    }

    public TelnyxRecordingCommandResponseDTO pauseRecording(String callControlId, TelnyxRecordingCommandRequestDTO body) {
        return getClient()
                .post()
                .uri("/v2/calls/{id}/actions/record_pause", callControlId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(TelnyxRecordingCommandResponseDTO.class);
    }

    public TelnyxRecordingCommandResponseDTO resumeRecording(String callControlId, TelnyxRecordingCommandRequestDTO body) {
        return getClient()
                .post()
                .uri("/v2/calls/{id}/actions/record_resume", callControlId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(TelnyxRecordingCommandResponseDTO.class);
    }

    public TelnyxRecordingCommandResponseDTO stopRecording(String callControlId, TelnyxRecordingCommandRequestDTO body) {
        return getClient()
                .post()
                .uri("/v2/calls/{id}/actions/record_stop", callControlId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(TelnyxRecordingCommandResponseDTO.class);
    }
}
