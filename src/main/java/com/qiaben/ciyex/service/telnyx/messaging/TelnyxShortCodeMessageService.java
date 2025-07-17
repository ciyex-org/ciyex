package com.qiaben.ciyex.service.telnyx.messaging;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxShortCodeMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxShortCodeMessageService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient;

    public Object sendShortCodeMessage(TelnyxShortCodeMessageDto payload) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/messages/short_code";

        return restClient
                .post()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Object.class);
    }
}
