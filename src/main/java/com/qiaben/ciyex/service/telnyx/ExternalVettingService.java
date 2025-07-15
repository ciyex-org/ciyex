package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class ExternalVettingService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient = RestClient.create();

    public ExternalVettingResponseDto importVetting(String brandId, ExternalVettingRequestDto body) {
        String url = "%s/v2/10dlc/brands/%s/external_vetting".formatted(telnyxProperties.getApiBaseUrl(), brandId);
        return restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(ExternalVettingResponseDto.class);
    }

    public Object orderVetting(String brandId, VettingClassRequestDto body) {
        String url = "%s/v2/10dlc/brands/%s/order_external_vetting".formatted(telnyxProperties.getApiBaseUrl(), brandId);
        return restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Object.class);
    }

    public BrandFeedbackResponseDto getFeedback(String brandId) {
        String url = "%s/v2/10dlc/brands/%s/feedback".formatted(telnyxProperties.getApiBaseUrl(), brandId);
        return restClient.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(BrandFeedbackResponseDto.class);
    }
}
