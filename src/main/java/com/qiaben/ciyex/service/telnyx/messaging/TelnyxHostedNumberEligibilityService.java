// src/main/java/com/qiaben/ciyex/service/telnyx/MessagingHostedNumberOrderService.java
package com.qiaben.ciyex.service.telnyx.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxHostedNumberEligibilityRequestDto;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxHostedNumberEligibilityResponseDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxErrorDto;
import com.qiaben.ciyex.config.TelnyxProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;

@Service
@RequiredArgsConstructor
public class TelnyxHostedNumberEligibilityService {

    private final RestClient restClient;
    private final TelnyxProperties props;
    private final ObjectMapper objectMapper;

    public TelnyxHostedNumberEligibilityResponseDto checkEligibility(
            TelnyxHostedNumberEligibilityRequestDto payload) {

        String url = props.getApiBaseUrl() + "/v2/messaging_hosted_numbers/eligibility";

        try {
            return restClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + props.getApiKey())
                    .body(payload)
                    .retrieve()
                    .body(TelnyxHostedNumberEligibilityResponseDto.class);

        } catch (HttpClientErrorException ex) {
            try {
                TelnyxErrorDto error = objectMapper.readValue(ex.getResponseBodyAsByteArray(), TelnyxErrorDto.class);
                throw new IllegalStateException(String.format("Telnyx error %d: %s – %s",
                        error.getCode(), error.getTitle(), error.getDetail()), ex);
            } catch (Exception parsingEx) {
                throw new IllegalStateException("Failed to parse Telnyx error response", parsingEx);
            }
        }
    }
}
