package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.TextToSpeechDto.GenerateSpeechRequest;
import com.qiaben.ciyex.dto.telnyx.TextToSpeechDto.VoiceListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TextToSpeechService {

    private final RestClient restClient;
    private final TelnyxProperties telnyxProperties;

    public VoiceListResponse listVoices(String provider, String apiKeyRef) {
        RestClient.RequestHeadersSpec<?> spec = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(telnyxProperties.getApiBaseUrl() + "/text-to-speech/voices")
                        .queryParamIfPresent("provider", java.util.Optional.ofNullable(provider))
                        .queryParamIfPresent("elevenlabs_api_key_ref", java.util.Optional.ofNullable(apiKeyRef))
                        .build())
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey());

        return spec.retrieve().body(VoiceListResponse.class);
    }

    public byte[] generateSpeech(GenerateSpeechRequest request) {
        return restClient.post()
                .uri(telnyxProperties.getApiBaseUrl() + "/text-to-speech/speech")
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .body(request)
                .retrieve()
                .body(byte[].class);
    }
}
