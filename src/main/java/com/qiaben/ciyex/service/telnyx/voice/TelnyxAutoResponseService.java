package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxAutoResponseDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxAutoResponseListResponse;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxAutoResponseRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxAutoResponseService {

    private final TelnyxProperties props;

    private RestClient client() {
        return RestClient.builder()
                .baseUrl(props.getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + props.getApiKey())
                .build();
    }

    public TelnyxAutoResponseListResponse list(String profileId, String countryCode,
                                               String createdAfter, String createdBefore,
                                               String updatedAfter, String updatedBefore) {
        return client()
                .get()
                .uri(uriBuilder -> {
                    var b = uriBuilder.path("/messaging_profiles/{profile_id}/auto_response_configs");
                    b.queryParam("country_code", countryCode);
                    b.queryParam("created_at[gte]", createdAfter);
                    b.queryParam("created_at[lte]", createdBefore);
                    b.queryParam("updated_at[gte]", updatedAfter);
                    b.queryParam("updated_at[lte]", updatedBefore);
                    return b.build(profileId);
                })
                .retrieve()
                .body(TelnyxAutoResponseListResponse.class);
    }

    public TelnyxAutoResponseDTO create(String profileId, TelnyxAutoResponseRequest request) {
        return client()
                .post()
                .uri("/messaging_profiles/{profile_id}/auto_response_configs", profileId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(TelnyxAutoResponseDTO.class);
    }

    public TelnyxAutoResponseDTO getById(String profileId, String cfgId) {
        return client()
                .get()
                .uri("/messaging_profiles/{profile_id}/auto_response_configs/{id}", profileId, cfgId)
                .retrieve()
                .body(TelnyxAutoResponseDTO.class);
    }

    public TelnyxAutoResponseDTO update(String profileId, String cfgId, TelnyxAutoResponseRequest request) {
        return client()
                .patch()
                .uri("/messaging_profiles/{profile_id}/auto_response_configs/{id}", profileId, cfgId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(TelnyxAutoResponseDTO.class);
    }

    public void delete(String profileId, String cfgId) {
        client()
                .delete()
                .uri("/messaging_profiles/{profile_id}/auto_response_configs/{id}", profileId, cfgId)
                .retrieve()
                .toBodilessEntity();
    }
}
