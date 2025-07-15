package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class CallSupervisorService {

    private final TelnyxProperties telnyxProperties;

    private RestClient client() {
        return RestClient.builder()
                .baseUrl(telnyxProperties.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .build();
    }

    public String switchRole(String callId, SupervisorRoleSwitchDTO dto) {
        return client().post()
                .uri("/calls/{id}/actions/switch_supervisor_role", callId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .body(String.class);
    }

    public String startTranscription(String callId, TranscriptionStartDTO dto) {
        return client().post()
                .uri("/calls/{id}/actions/transcription_start", callId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .body(String.class);
    }

    public String stopTranscription(String callId, TranscriptionStopDTO dto) {
        return client().post()
                .uri("/calls/{id}/actions/transcription_stop", callId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .body(String.class);
    }

    public String transferCall(String callId, TransferCallDTO dto) {
        return client().post()
                .uri("/calls/{id}/actions/transfer", callId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .body(String.class);
    }
}
