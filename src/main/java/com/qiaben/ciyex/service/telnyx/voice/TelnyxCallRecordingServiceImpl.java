package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxRecordingDto;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxRecordingListResponseDto;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxRecordingTranscriptionDto;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxRecordingTranscriptionListResponseDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxCustomStorageCredentialDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TelnyxCallRecordingServiceImpl implements TelnyxCallRecordingService {

    private final TelnyxProperties props;
    private RestClient client;

    private RestClient client() {
        if (client == null) {
            client = RestClient.builder()
                    .baseUrl(props.getApiBaseUrl())
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                    .build();
        }
        return client;
    }

    /* Recordings */
    @Override
    public TelnyxRecordingListResponseDto listRecordings(String queryString) {
        String path = "/v2/recordings" + (queryString == null ? "" : queryString);
        return client().get().uri(path).retrieve().body(TelnyxRecordingListResponseDto.class);
    }

    @Override
    public TelnyxRecordingDto getRecording(String recordingId) {
        return client().get()
                .uri("/v2/recordings/{id}", recordingId)
                .retrieve().body(TelnyxRecordingDto.class);
    }

    @Override
    public void deleteRecording(String recordingId) {
        client().delete().uri("/v2/recordings/{id}", recordingId).retrieve();
    }

    @Override
    public void deleteRecordingsBatch(List<String> recordingIds) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<List<String>> entity = new HttpEntity<>(recordingIds, headers);

        client().method(HttpMethod.DELETE)
                .uri("/v2/recordings/actions/delete")
                .body(entity)
                .retrieve();
    }


    /* Transcriptions */
    @Override
    public TelnyxRecordingTranscriptionListResponseDto listTranscriptions() {
        return client().get().uri("/v2/recording_transcriptions")
                .retrieve().body(TelnyxRecordingTranscriptionListResponseDto.class);
    }

    @Override
    public TelnyxRecordingTranscriptionDto getTranscription(String transcriptionId) {
        return client().get().uri("/v2/recording_transcriptions/{id}", transcriptionId)
                .retrieve().body(TelnyxRecordingTranscriptionDto.class);
    }

    @Override
    public void deleteTranscription(String transcriptionId) {
        client().delete().uri("/v2/recording_transcriptions/{id}", transcriptionId).retrieve();
    }

    /* Custom Storage Credentials */
    @Override
    public TelnyxCustomStorageCredentialDto getCredentials(String connectionId) {
        return client().get().uri("/v2/custom_storage_credentials/{id}", connectionId)
                .retrieve().body(TelnyxCustomStorageCredentialDto.class);
    }

    @Override
    public TelnyxCustomStorageCredentialDto createCredentials(String connectionId, TelnyxCustomStorageCredentialDto body) {
        return client().post().uri("/v2/custom_storage_credentials/{id}", connectionId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve().body(TelnyxCustomStorageCredentialDto.class);
    }

    @Override
    public TelnyxCustomStorageCredentialDto updateCredentials(String connectionId, TelnyxCustomStorageCredentialDto body) {
        return client().put().uri("/v2/custom_storage_credentials/{id}", connectionId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve().body(TelnyxCustomStorageCredentialDto.class);
    }

    @Override
    public void deleteCredentials(String connectionId) {
        client().delete().uri("/v2/custom_storage_credentials/{id}", connectionId).retrieve();
    }
}
