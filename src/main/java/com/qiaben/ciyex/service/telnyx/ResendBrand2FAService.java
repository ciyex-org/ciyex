package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.ResendBrand2FAResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class ResendBrand2FAService {

    private final RestClient restClient;
    private final TelnyxProperties properties;

    public ResendBrand2FAResponseDto resend2FAEmail(String brandId) {
        String url = properties.getApiBaseUrl() + "/v2/brands/" + brandId + "/resend_2fa_email";

        return restClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .retrieve()
                .body(ResendBrand2FAResponseDto.class);
    }
}