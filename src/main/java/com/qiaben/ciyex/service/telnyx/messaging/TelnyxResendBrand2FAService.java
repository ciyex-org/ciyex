package com.qiaben.ciyex.service.telnyx.messaging;

import com.qiaben.ciyex.config.TelnyxProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxResendBrand2FAService {

    private final RestClient restClient;
    private final TelnyxProperties properties;

    public TelnyxResendBrand2FAResponseDto resend2FAEmail(String brandId) {
        String url = properties.getApiBaseUrl() + "/v2/brands/" + brandId + "/resend_2fa_email";

        return restClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .retrieve()
                .body(TelnyxResendBrand2FAResponseDto.class);
    }
}