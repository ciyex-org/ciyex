package com.qiaben.ciyex.service.telnyx.messaging;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxRetrieveMessagingProfileDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxRetrieveMessagingProfileService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient;

    public TelnyxRetrieveMessagingProfileDto getMessagingProfile(String id) {
        String url = String.format("%s/v2/messaging_profiles/%s", telnyxProperties.getApiBaseUrl(), id);

        return restClient
                .get()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxRetrieveMessagingProfileDto.class);
    }
}
