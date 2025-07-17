package com.qiaben.ciyex.service.telnyx.video;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxRecordingTranscriptionDto;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxRecordingTranscriptionListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxRecordingTranscriptionService {

    private final RestClient restClient;
    private final TelnyxProperties telnyxProperties;

    public TelnyxRecordingTranscriptionListResponseDto list(String accountSid, Integer pageSize, String pageToken) {
        String uri = telnyxProperties.getApiBaseUrl()
                + "/v2/texml/Accounts/" + accountSid + "/RecordingTranscriptions"
                + "?PageSize=" + pageSize + (pageToken != null ? "&PageToken=" + pageToken : "");

        return restClient.get()
                .uri(uri)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxRecordingTranscriptionListResponseDto.class);
    }

    public TelnyxRecordingTranscriptionDto get(String accountSid, String transcriptionSid) {
        String uri = telnyxProperties.getApiBaseUrl()
                + "/v2/texml/Accounts/" + accountSid + "/RecordingTranscriptions/" + transcriptionSid;

        return restClient.get()
                .uri(uri)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxRecordingTranscriptionDto.class);
    }

    public void delete(String accountSid, String transcriptionSid) {
        String uri = telnyxProperties.getApiBaseUrl()
                + "/v2/texml/Accounts/" + accountSid + "/RecordingTranscriptions/" + transcriptionSid;

        restClient.delete()
                .uri(uri)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve();
    }
}
