package com.qiaben.ciyex.service.telnyx;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.HostedNumberEligibilityRequestDto;
import com.qiaben.ciyex.dto.telnyx.HostedNumberEligibilityResponseDto;
import com.qiaben.ciyex.dto.telnyx.TelnyxErrorDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class MessagingHostedNumberOrderService {

    private final RestClient restClient;
    private final TelnyxProperties props;
    private final ObjectMapper objectMapper;

    public HostedNumberEligibilityResponseDto checkEligibility(
            HostedNumberEligibilityRequestDto payload) {

        String url = props.getApiBaseUrl() + "/v2/messaging_hosted_numbers/eligibility";

        try {
            return restClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + props.getApiKey())
                    .body(payload)
                    .retrieve()
                    .body(HostedNumberEligibilityResponseDto.class);

        } catch (HttpClientErrorException ex) {
            throw wrapTelnyxError(ex);
        }
    }

    private IllegalStateException wrapTelnyxError(HttpClientErrorException ex) {
        try {
            TelnyxErrorDto error = objectMapper.readValue(
                    ex.getResponseBodyAsByteArray(), TelnyxErrorDto.class);
            return new IllegalStateException(String.format("Telnyx error %d: %s – %s",
                    error.getCode(), error.getTitle(), error.getDetail()), ex);
        } catch (Exception parseFailure) {
            return new IllegalStateException("Telnyx call failed: " + ex.getStatusCode(), ex);
        }
    }
}
