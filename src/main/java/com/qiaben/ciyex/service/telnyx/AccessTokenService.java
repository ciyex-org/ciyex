package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.AccessTokenResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class AccessTokenService {

    private final TelnyxProperties props;

    public AccessTokenResponseDto createAccessToken(String credentialId) {
        RestClient client = RestClient.builder()
                .baseUrl(props.getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + props.getApiKey())
                .build();

        String jwt = client.post()
                .uri("/v2/telephony_credentials/{id}/token", credentialId)
                .retrieve()
                .body(String.class);

        return new AccessTokenResponseDto(jwt);
    }
}

