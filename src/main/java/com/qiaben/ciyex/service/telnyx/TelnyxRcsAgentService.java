package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.RcsAgentDTO;
import com.qiaben.ciyex.dto.telnyx.RcsAgentListResponse;
import com.qiaben.ciyex.dto.telnyx.RcsAgentUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxRcsAgentService {

    private final TelnyxProperties props;

    private RestClient client() {
        return RestClient.builder()
                .baseUrl(props.getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + props.getApiKey())
                .build();
    }

    public RcsAgentListResponse listAgents(int pageNumber, int pageSize) {
        return client()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/rcs/agents")
                        .queryParam("page[number]", pageNumber)
                        .queryParam("page[size]", pageSize)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(RcsAgentListResponse.class);
    }

    public RcsAgentDTO getAgentById(String id) {
        return client()
                .get()
                .uri("/rcs/agents/{id}", id)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(RcsAgentDTO.class);
    }

    public RcsAgentDTO updateAgent(String id, RcsAgentUpdateRequest request) {
        return client()
                .patch()
                .uri("/rcs/agents/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(RcsAgentDTO.class);
    }
}
