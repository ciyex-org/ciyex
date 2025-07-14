package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.TelnyxAiAssistantRequestDTO;
import com.qiaben.ciyex.dto.telnyx.TelnyxAiAssistantResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxCallAiAssistantStartService {

    private final TelnyxProperties telnyxProperties;

    public TelnyxAiAssistantResponseDTO startAiAssistant(String callControlId, TelnyxAiAssistantRequestDTO request) {
        RestClient restClient = RestClient.builder()
                .baseUrl(telnyxProperties.getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .build();

        return restClient.post()
                .uri("/v2/calls/{call_control_id}/actions/ai_assistant_start", callControlId)
                .body(request)
                .retrieve()
                .body(TelnyxAiAssistantResponseDTO.class);
    }
}
