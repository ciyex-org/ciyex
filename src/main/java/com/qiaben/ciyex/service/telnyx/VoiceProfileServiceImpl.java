package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.UpdateVoiceProfileRequestDto;
import com.qiaben.ciyex.dto.telnyx.UpdateVoiceProfileResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;

@Service
@RequiredArgsConstructor
public class VoiceProfileServiceImpl implements VoiceProfileService {

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
    public UpdateVoiceProfileResponseDto updateVoiceProfile(String brandId, UpdateVoiceProfileRequestDto request) {
        return client()
                .patch()
                .uri("/v2/10dlc/brand/" + brandId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(UpdateVoiceProfileResponseDto.class);
    }
}
