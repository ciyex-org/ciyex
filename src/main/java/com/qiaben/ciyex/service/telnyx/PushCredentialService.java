package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.PushCredentialResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PushCredentialService {

    private final TelnyxProperties telnyxProperties;

    public PushCredentialResponseDTO getPushCredentialById(String id) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/mobile_push_credentials/" + id;

        RestClient client = RestClient.builder()
                .defaultHeader("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .build();

        try {
            Map<?, ?> response = client.get()
                    .uri(url)
                    .retrieve()
                    .body(Map.class); // expected: { data: {...} }

            Map<?, ?> data = (Map<?, ?>) response.get("data");

            PushCredentialResponseDTO dto = new PushCredentialResponseDTO();
            dto.setId((String) data.get("id"));
            dto.setCertificate((String) data.get("certificate"));
            dto.setPrivate_key((String) data.get("private_key"));
            dto.setProject_account_json_file((Map<String, Object>) data.get("project_account_json_file"));
            dto.setAlias((String) data.get("alias"));
            dto.setType((String) data.get("type"));
            dto.setRecord_type((String) data.get("record_type"));
            // skip parsing created_at, updated_at for simplicity
            return dto;

        } catch (RestClientResponseException e) {
            throw new RuntimeException("❌ Failed to fetch push credential: " + e.getResponseBodyAsString(), e);
        }
    }
}
