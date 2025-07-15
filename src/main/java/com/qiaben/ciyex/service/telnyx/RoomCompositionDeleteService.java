package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
@RequiredArgsConstructor
public class RoomCompositionDeleteService {

    private final RestClient restClient;
    private final TelnyxProperties telnyxProperties;

    public boolean deleteRoomComposition(String roomCompositionId) {
        try {
            restClient.delete()
                    .uri(telnyxProperties.getApiBaseUrl() + "/v2/room_compositions/{id}", roomCompositionId)
                    .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            throw ex;
        }
    }
}
