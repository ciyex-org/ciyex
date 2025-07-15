package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.PartnerCampaignSharedByMeResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class GetPartnerCampaignsSharedByMeService {

    private final TelnyxProperties telnyxProperties;

    public PartnerCampaignSharedByMeResponseDTO getCampaigns(int page, int recordsPerPage) {
        String url = UriComponentsBuilder.fromHttpUrl(telnyxProperties.getApiBaseUrl() + "/10dlc/partnerCampaign/sharedByMe")
                .queryParam("page", page)
                .queryParam("recordsPerPage", recordsPerPage)
                .toUriString();

        RestClient restClient = RestClient.builder().build();

        return restClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(PartnerCampaignSharedByMeResponseDTO.class);
    }
}
