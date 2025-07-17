package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTeXmlSecretRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTeXmlSecretResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxTeXmlSecretService {

    private final RestClient restClient;
    private final TelnyxProperties telnyxProperties;

    public TelnyxTeXmlSecretResponseDto createSecret(TelnyxTeXmlSecretRequestDto requestDto) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/texml/secrets";

        return restClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.valueOf("application/json"))
                .body(requestDto)
                .retrieve()
                .body(TelnyxTeXmlSecretResponseDto.class);
    }
}
