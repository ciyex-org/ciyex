package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxSupervisorRoleSwitchDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTranscriptionStartDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTranscriptionStopDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTransferCallDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxCallSupervisorService {

    private final TelnyxProperties telnyxProperties;

    private RestClient client() {
        return RestClient.builder()
                .baseUrl(telnyxProperties.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .build();
    }

    public String switchRole(String callId, TelnyxSupervisorRoleSwitchDTO dto) {
        return client().post()
                .uri("/calls/{id}/actions/switch_supervisor_role", callId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .body(String.class);
    }

    public String startTranscription(String callId, TelnyxTranscriptionStartDTO dto) {
        return client().post()
                .uri("/calls/{id}/actions/transcription_start", callId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .body(String.class);
    }

    public String stopTranscription(String callId, TelnyxTranscriptionStopDTO dto) {
        return client().post()
                .uri("/calls/{id}/actions/transcription_stop", callId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .body(String.class);
    }

    public String transferCall(String callId, TelnyxTransferCallDTO dto) {
        return client().post()
                .uri("/calls/{id}/actions/transfer", callId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .body(String.class);
    }
}
