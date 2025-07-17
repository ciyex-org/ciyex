package com.qiaben.ciyex.service.telnyx.video;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxRoomRecordingViewResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxRoomRecordingViewDeleteService {

    private final RestClient restClient;
    private final TelnyxProperties telnyxProperties;

    public TelnyxRoomRecordingViewResponseDto viewRecording(String id) {
        String url = String.format("%s/v2/room_recordings/%s", telnyxProperties.getApiBaseUrl(), id);

        return restClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxRoomRecordingViewResponseDto.class);
    }

    public void deleteRecording(String id) {
        String url = String.format("%s/v2/room_recordings/%s", telnyxProperties.getApiBaseUrl(), id);

        try {
            restClient.delete()
                    .uri(url)
                    .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new RuntimeException("Room recording not found.");
        }
    }
}
