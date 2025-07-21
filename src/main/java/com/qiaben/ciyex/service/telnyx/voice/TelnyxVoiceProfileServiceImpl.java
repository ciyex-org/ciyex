package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxUpdateVoiceProfileRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxUpdateVoiceProfileResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;

@Service
@RequiredArgsConstructor
public class TelnyxVoiceProfileServiceImpl implements TelnyxVoiceProfileService {

    private final TelnyxProperties props;

    private RestClient client;

    private RestClient client() {
        if (client == null) {
            client = RestClient.builder()
                    .baseUrl(props.getApiBaseUrl())
                    .defaultHeader("Authorization", "Bearer " + props.getApiKey())
                    .build();
        }
        return client;
    }

    @Override
    public TelnyxUpdateVoiceProfileResponseDto updateVoiceProfile(String brandId, TelnyxUpdateVoiceProfileRequestDto request) {
        return client()
                .patch()
                .uri("/v2/10dlc/brand/" + brandId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(TelnyxUpdateVoiceProfileResponseDto.class);
    }
}
