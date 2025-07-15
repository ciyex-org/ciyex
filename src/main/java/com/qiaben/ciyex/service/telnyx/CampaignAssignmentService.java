package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class CampaignAssignmentService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient;

    // -------- POST /v2/messaging_profile_campaign_assignments ----------
    public AssignMessagingProfileResponseDTO assignMessagingProfile(AssignMessagingProfileRequestDTO body) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/messaging_profile_campaign_assignments";
        return restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .body(body)
                .retrieve()
                .body(AssignMessagingProfileResponseDTO.class);
    }

    // -------- GET /v2/messaging_profile_campaign_assignments/{taskId} ----------
    public TaskStatusResponseDTO getTaskStatus(String taskId) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/messaging_profile_campaign_assignments/" + taskId;
        return restClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TaskStatusResponseDTO.class);
    }

    // -------- GET /v2/messaging_profile_campaign_assignments/{taskId}/phone_number_status ----------
    public PhoneNumberStatusListResponseDTO getPhoneNumberStatuses(String taskId, Integer page, Integer recordsPerPage) {
        String base = telnyxProperties.getApiBaseUrl() + "/v2/messaging_profile_campaign_assignments/" + taskId + "/phone_number_status";
        String url = UriComponentsBuilder.fromHttpUrl(base)
                .queryParam("page", page)
                .queryParam("recordsPerPage", recordsPerPage)
                .toUriString();

        return restClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(PhoneNumberStatusListResponseDTO.class);
    }
}
