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
public class CallControlService {

    private final TelnyxProperties properties;

    private RestClient client() {
        return RestClient.builder()
                .baseUrl(properties.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .build();
    }

    public GenericTelnyxResponseDTO sendDtmf(String callControlId, SendDtmfRequestDTO request) {
        return client().post()
                .uri("/v2/calls/{id}/actions/send_dtmf", callControlId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(GenericTelnyxResponseDTO.class);
    }

    public GenericTelnyxResponseDTO sendSipInfo(String callControlId, SendSipInfoRequestDTO request) {
        return client().post()
                .uri("/v2/calls/{id}/actions/send_sip_info", callControlId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(GenericTelnyxResponseDTO.class);
    }

    public GenericTelnyxResponseDTO speakText(String callControlId, SpeakTextRequestDTO request) {
        return client().post()
                .uri("/v2/calls/{id}/actions/speak", callControlId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(GenericTelnyxResponseDTO.class);
    }
}

