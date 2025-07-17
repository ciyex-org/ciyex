package com.qiaben.ciyex.service.telnyx.messaging;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxRetrieveMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxRetrieveMessageService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient;

    public TelnyxRetrieveMessageDto getMessageById(String id) {
        String url = String.format("%s/v2/messages/%s", telnyxProperties.getApiBaseUrl(), id);

        return restClient
                .get()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(TelnyxRetrieveMessageDto.class);
    }
}
