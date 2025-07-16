package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.RetrieveMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class RetrieveMessageService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient;

    public RetrieveMessageDto getMessageById(String id) {
        String url = String.format("%s/v2/messages/%s", telnyxProperties.getApiBaseUrl(), id);

        return restClient
                .get()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(RetrieveMessageDto.class);
    }
}
