package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxGenericTelnyxResponseDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxSendDtmfRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxSendSipInfoRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxSpeakTextRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxCallControlService {

    private final TelnyxProperties properties;

    private RestClient client() {
        return RestClient.builder()
                .baseUrl(properties.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .build();
    }

    public TelnyxGenericTelnyxResponseDTO sendDtmf(String callControlId, TelnyxSendDtmfRequestDTO request) {
        return client().post()
                .uri("/v2/calls/{id}/actions/send_dtmf", callControlId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(TelnyxGenericTelnyxResponseDTO.class);
    }

    public TelnyxGenericTelnyxResponseDTO sendSipInfo(String callControlId, TelnyxSendSipInfoRequestDTO request) {
        return client().post()
                .uri("/v2/calls/{id}/actions/send_sip_info", callControlId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(TelnyxGenericTelnyxResponseDTO.class);
    }

    public TelnyxGenericTelnyxResponseDTO speakText(String callControlId, TelnyxSpeakTextRequestDTO request) {
        return client().post()
                .uri("/v2/calls/{id}/actions/speak", callControlId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(TelnyxGenericTelnyxResponseDTO.class);
    }
}

