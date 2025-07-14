package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.QueueDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class QueueService {

    private final RestClient restClient;
    private final TelnyxProperties telnyxProperties;

    public QueueDto.QueueWrapper getQueue(String queueName) {
        return restClient.get()
                .uri(telnyxProperties.getApiBaseUrl() + "/v2/queues/" + queueName)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(QueueDto.QueueWrapper.class);
    }

    public QueueDto.QueueCallWrapper getQueueCall(String queueName, String callControlId) {
        return restClient.get()
                .uri(telnyxProperties.getApiBaseUrl() + "/v2/queues/" + queueName + "/calls/" + callControlId)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(QueueDto.QueueCallWrapper.class);
    }

    public QueueDto.QueueCallListWrapper listQueueCalls(String queueName, Integer page, Integer size) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(telnyxProperties.getApiBaseUrl() + "/v2/queues/" + queueName + "/calls")
                        .queryParam("page[number]", page)
                        .queryParam("page[size]", size)
                        .build())
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(QueueDto.QueueCallListWrapper.class);
    }
}
