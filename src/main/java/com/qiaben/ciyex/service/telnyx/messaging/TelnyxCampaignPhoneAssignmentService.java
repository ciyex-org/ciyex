package com.qiaben.ciyex.service.telnyx.messaging;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxCampaignPhoneAssignmentListResponseDTO;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxCampaignPhoneAssignmentRequestDTO;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxCampaignPhoneAssignmentResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class TelnyxCampaignPhoneAssignmentService {

    private final TelnyxProperties properties;
    private final RestClient restClient;

    public TelnyxCampaignPhoneAssignmentResponseDTO assignPhoneToCampaign(TelnyxCampaignPhoneAssignmentRequestDTO body) {
        String url = properties.getApiBaseUrl() + "/v2/campaign_phone_number_assignments";
        return restClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .body(body)
                .retrieve()
                .body(TelnyxCampaignPhoneAssignmentResponseDTO.class);
    }

    public TelnyxCampaignPhoneAssignmentListResponseDTO getAssignments(Integer page, Integer recordsPerPage,
                                                                       String telnyxCampaignId, String telnyxBrandId, String tcrCampaignId, String tcrBrandId, String sort) {

        String base = properties.getApiBaseUrl() + "/v2/campaign_phone_number_assignments";
        String url = UriComponentsBuilder.fromHttpUrl(base)
                .queryParam("page", page)
                .queryParam("recordsPerPage", recordsPerPage)
                .queryParam("filter[telnyx_campaign_id]", telnyxCampaignId)
                .queryParam("filter[telnyx_brand_id]", telnyxBrandId)
                .queryParam("filter[tcr_campaign_id]", tcrCampaignId)
                .queryParam("filter[tcr_brand_id]", tcrBrandId)
                .queryParam("sort", sort)
                .toUriString();

        return restClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .retrieve()
                .body(TelnyxCampaignPhoneAssignmentListResponseDTO.class);
    }

    public TelnyxCampaignPhoneAssignmentResponseDTO getAssignmentByPhoneNumber(String phoneNumber) {
        String url = properties.getApiBaseUrl() + "/v2/campaign_phone_number_assignments/" + phoneNumber;
        return restClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .retrieve()
                .body(TelnyxCampaignPhoneAssignmentResponseDTO.class);
    }

    public TelnyxCampaignPhoneAssignmentResponseDTO updateAssignmentByPhoneNumber(String phoneNumber, TelnyxCampaignPhoneAssignmentRequestDTO body) {
        String url = properties.getApiBaseUrl() + "/v2/campaign_phone_number_assignments/" + phoneNumber;
        return restClient.put()
                .uri(url)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .body(body)
                .retrieve()
                .body(TelnyxCampaignPhoneAssignmentResponseDTO.class);
    }
}
