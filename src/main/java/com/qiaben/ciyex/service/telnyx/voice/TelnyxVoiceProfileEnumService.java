package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxVoiceProfileEnumDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TelnyxVoiceProfileEnumService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient = RestClient.create();

    public TelnyxVoiceProfileEnumDto getEnumValues(String endpoint) {
        String url = "%s/v2/voice_profiles/%s".formatted(telnyxProperties.getApiBaseUrl(), endpoint);

        String[] values = restClient.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String[].class);

        TelnyxVoiceProfileEnumDto dto = new TelnyxVoiceProfileEnumDto();
        dto.setValues(values != null ? List.of(values) : List.of());
        return dto;
    }
}
