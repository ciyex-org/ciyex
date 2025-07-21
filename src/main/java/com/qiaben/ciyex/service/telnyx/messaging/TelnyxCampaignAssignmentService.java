package com.qiaben.ciyex.service.telnyx.messaging;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxAssignMessagingProfileRequestDTO;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxAssignMessagingProfileResponseDTO;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxPhoneNumberStatusListResponseDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTaskStatusResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class TelnyxCampaignAssignmentService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient;


    public TelnyxAssignMessagingProfileResponseDTO assignMessagingProfile(TelnyxAssignMessagingProfileRequestDTO body) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/messaging_profile_campaign_assignments";
        return restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .body(body)
                .retrieve()
                .body(TelnyxAssignMessagingProfileResponseDTO.class);
    }

    public TelnyxTaskStatusResponseDTO getTaskStatus(String taskId) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/messaging_profile_campaign_assignments/" + taskId;
        return restClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxTaskStatusResponseDTO.class);
    }


    public TelnyxPhoneNumberStatusListResponseDTO getPhoneNumberStatuses(String taskId, Integer page, Integer recordsPerPage) {
        String base = telnyxProperties.getApiBaseUrl() + "/v2/messaging_profile_campaign_assignments/" + taskId + "/phone_number_status";
        String url = UriComponentsBuilder.fromHttpUrl(base)
                .queryParam("page", page)
                .queryParam("recordsPerPage", recordsPerPage)
                .toUriString();

        return restClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxPhoneNumberStatusListResponseDTO.class);
    }
}
