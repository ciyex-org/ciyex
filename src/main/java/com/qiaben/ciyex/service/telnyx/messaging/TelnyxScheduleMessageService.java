package com.qiaben.ciyex.service.telnyx.messaging;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxScheduleMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxScheduleMessageService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient;

    public Object scheduleMessage(TelnyxScheduleMessageDto dto) {
        String url = String.format("%s/v2/messages/schedule", telnyxProperties.getApiBaseUrl());

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
