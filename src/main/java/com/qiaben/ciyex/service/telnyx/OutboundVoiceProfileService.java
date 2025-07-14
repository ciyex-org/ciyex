package com.qiaben.ciyex.service.telnyx;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.OutboundVoiceProfileDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OutboundVoiceProfileService {

    private final TelnyxProperties telnyxProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private RestClient client() {
        return RestClient.builder()
                .baseUrl(telnyxProperties.getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .build();
    }

    public OutboundVoiceProfileDTO retrieveById(Long id) {
        try {
            Map<String, Object> response = client().get()
                    .uri("/v2/outbound_voice_profiles/{id}", id)
                    .retrieve()
                    .body(Map.class);
            return objectMapper.convertValue(response.get("data"), OutboundVoiceProfileDTO.class);
        } catch (RestClientResponseException e) {
            throw new RuntimeException("Retrieve failed: " + e.getResponseBodyAsString(), e);
        }
    }

    public List<OutboundVoiceProfileDTO> listAll() {
        try {
            Map<String, Object> response = client().get()
                    .uri("/v2/outbound_voice_profiles")
                    .retrieve()
                    .body(Map.class);
            return objectMapper.convertValue(response.get("data"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, OutboundVoiceProfileDTO.class));
        } catch (RestClientResponseException e) {
            throw new RuntimeException("List failed: " + e.getResponseBodyAsString(), e);
        }
    }

    public OutboundVoiceProfileDTO create(OutboundVoiceProfileDTO dto) {
        try {
            Map<String, Object> response = client().post()
                    .uri("/v2/outbound_voice_profiles")
                    .body(dto)
                    .retrieve()
                    .body(Map.class);
            return objectMapper.convertValue(response.get("data"), OutboundVoiceProfileDTO.class);
        } catch (RestClientResponseException e) {
            throw new RuntimeException("Create failed: " + e.getResponseBodyAsString(), e);
        }
    }

    public OutboundVoiceProfileDTO update(Long id, OutboundVoiceProfileDTO dto) {
        try {
            Map<String, Object> response = client().method(HttpMethod.PATCH)
                    .uri("/v2/outbound_voice_profiles/{id}", id)
                    .body(dto)
                    .retrieve()
                    .body(Map.class);
            return objectMapper.convertValue(response.get("data"), OutboundVoiceProfileDTO.class);
        } catch (RestClientResponseException e) {
            throw new RuntimeException("Update failed: " + e.getResponseBodyAsString(), e);
        }
    }

    public void delete(Long id) {
        try {
            client().delete()
                    .uri("/v2/outbound_voice_profiles/{id}", id)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new RuntimeException("Delete failed: " + e.getResponseBodyAsString(), e);
        }
    }
}
