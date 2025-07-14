package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.RecordingCommandRequestDTO;
import com.qiaben.ciyex.dto.telnyx.RecordingCommandResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class RecordingService {

    private final TelnyxProperties properties;

    private RestClient getClient() {
        return RestClient.builder()
                .baseUrl(properties.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .build();
    }

    public RecordingCommandResponseDTO startRecording(String callControlId, RecordingCommandRequestDTO body) {
        return getClient()
                .post()
                .uri("/v2/calls/{id}/actions/record_start", callControlId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(RecordingCommandResponseDTO.class);
    }

    public RecordingCommandResponseDTO pauseRecording(String callControlId, RecordingCommandRequestDTO body) {
        return getClient()
                .post()
                .uri("/v2/calls/{id}/actions/record_pause", callControlId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(RecordingCommandResponseDTO.class);
    }

    public RecordingCommandResponseDTO resumeRecording(String callControlId, RecordingCommandRequestDTO body) {
        return getClient()
                .post()
                .uri("/v2/calls/{id}/actions/record_resume", callControlId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(RecordingCommandResponseDTO.class);
    }

    public RecordingCommandResponseDTO stopRecording(String callControlId, RecordingCommandRequestDTO body) {
        return getClient()
                .post()
                .uri("/v2/calls/{id}/actions/record_stop", callControlId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(RecordingCommandResponseDTO.class);
    }
}
