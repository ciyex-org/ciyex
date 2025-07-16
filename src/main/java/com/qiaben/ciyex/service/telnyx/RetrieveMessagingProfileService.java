package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.RetrieveMessagingProfileDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class RetrieveMessagingProfileService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient;

    public RetrieveMessagingProfileDto getMessagingProfile(String id) {
        String url = String.format("%s/v2/messaging_profiles/%s", telnyxProperties.getApiBaseUrl(), id);

        return restClient
                .get()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(RetrieveMessagingProfileDto.class);
    }
}
