package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.UpdateMessagingProfileDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class UpdateMessagingProfileService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient;

    public UpdateMessagingProfileDto updateMessagingProfile(String id, UpdateMessagingProfileDto updateMessagingProfileDto) {
        String url = String.format("%s/v2/messaging_profiles/%s", telnyxProperties.getApiBaseUrl(), id);

        return restClient
                .patch()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(updateMessagingProfileDto)
                .retrieve()
                .body(UpdateMessagingProfileDto.class);
    }
}
