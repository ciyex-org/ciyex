package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class CampaignPhoneAssignmentService {

    private final TelnyxProperties properties;
    private final RestClient restClient;

    public CampaignPhoneAssignmentResponseDTO assignPhoneToCampaign(CampaignPhoneAssignmentRequestDTO body) {
        String url = properties.getApiBaseUrl() + "/v2/campaign_phone_number_assignments";
        return restClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .body(body)
                .retrieve()
                .body(CampaignPhoneAssignmentResponseDTO.class);
    }

    public CampaignPhoneAssignmentListResponseDTO getAssignments(Integer page, Integer recordsPerPage,
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
                .body(CampaignPhoneAssignmentListResponseDTO.class);
    }

    public CampaignPhoneAssignmentResponseDTO getAssignmentByPhoneNumber(String phoneNumber) {
        String url = properties.getApiBaseUrl() + "/v2/campaign_phone_number_assignments/" + phoneNumber;
        return restClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .retrieve()
                .body(CampaignPhoneAssignmentResponseDTO.class);
    }

    public CampaignPhoneAssignmentResponseDTO updateAssignmentByPhoneNumber(String phoneNumber, CampaignPhoneAssignmentRequestDTO body) {
        String url = properties.getApiBaseUrl() + "/v2/campaign_phone_number_assignments/" + phoneNumber;
        return restClient.put()
                .uri(url)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .body(body)
                .retrieve()
                .body(CampaignPhoneAssignmentResponseDTO.class);
    }
}
