package com.qiaben.ciyex.service.telnyx.messaging;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxCancelScheduleMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxCancelScheduleMessageService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient;

    public TelnyxCancelScheduleMessageDto cancelScheduledMessage(String id) {
        String url = String.format("%s/v2/messages/%s", telnyxProperties.getApiBaseUrl(), id);

        return restClient
                .delete()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(TelnyxCancelScheduleMessageDto.class);
    }
}
