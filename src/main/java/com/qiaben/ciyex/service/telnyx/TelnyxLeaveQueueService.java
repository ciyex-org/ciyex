package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.TelnyxLeaveQueueRequestDTO;
import com.qiaben.ciyex.dto.telnyx.TelnyxLeaveQueueResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
@RequiredArgsConstructor
public class TelnyxLeaveQueueService {

    private final TelnyxProperties telnyxProperties;

    public TelnyxLeaveQueueResponseDTO removeCallFromQueue(String callControlId, TelnyxLeaveQueueRequestDTO request) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/calls/" + callControlId + "/actions/leave_queue";

        RestClient restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        try {
            return restClient.method(HttpMethod.POST)
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(TelnyxLeaveQueueResponseDTO.class);
        } catch (RestClientResponseException ex) {
            TelnyxLeaveQueueResponseDTO errorResponse = new TelnyxLeaveQueueResponseDTO();
            TelnyxLeaveQueueResponseDTO.DataWrapper data = new TelnyxLeaveQueueResponseDTO.DataWrapper();
            data.setResult("Error: " + ex.getResponseBodyAsString());
            errorResponse.setData(data);
            return errorResponse;
        }
    }
}
