package com.qiaben.ciyex.service.telnyx.messaging;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxDeleteMessagingProfileDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxDeleteMessagingProfileService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient;

    public TelnyxDeleteMessagingProfileDto deleteMessagingProfile(String id) {
        String url = String.format("%s/v2/messaging_profiles/%s", telnyxProperties.getApiBaseUrl(), id);

        restClient
                .delete()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve();

        return new TelnyxDeleteMessagingProfileDto(id, "Deleted successfully");
    }
}
