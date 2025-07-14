package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.TelnyxAiAssistantRequestDTO;
import com.qiaben.ciyex.dto.telnyx.TelnyxAiAssistantResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelnyxAiAssistantService {

    private final TelnyxProperties telnyxProperties;

    public TelnyxAiAssistantResponseDTO startAi(String callControlId, TelnyxAiAssistantRequestDTO request) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/calls/" + callControlId + "/actions/ai_assistant_start";
        return sendAiCommand(url, request);
    }

    public TelnyxAiAssistantResponseDTO stopAi(String callControlId, TelnyxAiAssistantRequestDTO request) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/calls/" + callControlId + "/actions/ai_assistant_stop";
        return sendAiCommand(url, request);
    }

    private TelnyxAiAssistantResponseDTO sendAiCommand(String url, TelnyxAiAssistantRequestDTO request) {
        try {
            RestClient restClient = RestClient.create();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(telnyxProperties.getApiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<TelnyxAiAssistantRequestDTO> entity = new HttpEntity<>(request, headers);
            return restClient
                    .method(HttpMethod.POST)
                    .uri(url)
                    .headers(httpHeaders -> {
                        httpHeaders.setBearerAuth(telnyxProperties.getApiKey());
                        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                    })
                    .body(request)
                    .retrieve()
                    .body(TelnyxAiAssistantResponseDTO.class);
        } catch (RestClientResponseException ex) {
            log.error("Telnyx AI Assistant request failed: {} - {}", ex.getRawStatusCode(), ex.getResponseBodyAsString());
            throw ex;
        }
    }
}
