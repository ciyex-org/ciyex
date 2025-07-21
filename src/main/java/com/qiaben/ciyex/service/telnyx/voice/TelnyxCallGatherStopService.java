package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxGatherStopRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxGatherStopResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Service
@RequiredArgsConstructor
public class TelnyxCallGatherStopService {

    private final TelnyxProperties telnyxProperties;

    public TelnyxGatherStopResponseDTO stopGather(String callControlId, TelnyxGatherStopRequestDTO request) {
        RestClient restClient = RestClient.builder().baseUrl(telnyxProperties.getApiBaseUrl()).build();

        return restClient.post()
                .uri("/v2/calls/{call_control_id}/actions/gather_stop", callControlId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(TelnyxGatherStopResponseDTO.class);
    }
}
