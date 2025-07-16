package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.SendLongCodeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class SendLongCodeService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient;

    public Object sendLongCodeMessage(SendLongCodeDto dto) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/messages/long_code";

        return restClient
                .post()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .body(Object.class);
    }
}
