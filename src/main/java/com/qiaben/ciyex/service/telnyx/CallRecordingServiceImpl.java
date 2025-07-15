package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CallRecordingServiceImpl implements CallRecordingService {

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
    public RecordingListResponseDto listRecordings(String queryString) {
        String path = "/v2/recordings" + (queryString == null ? "" : queryString);
        return client().get().uri(path).retrieve().body(RecordingListResponseDto.class);
    }

    @Override
    public RecordingDto getRecording(String recordingId) {
        return client().get()
                .uri("/v2/recordings/{id}", recordingId)
                .retrieve().body(RecordingDto.class);
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
    public RecordingTranscriptionListResponseDto listTranscriptions() {
        return client().get().uri("/v2/recording_transcriptions")
                .retrieve().body(RecordingTranscriptionListResponseDto.class);
    }

    @Override
    public RecordingTranscriptionDto getTranscription(String transcriptionId) {
        return client().get().uri("/v2/recording_transcriptions/{id}", transcriptionId)
                .retrieve().body(RecordingTranscriptionDto.class);
    }

    @Override
    public void deleteTranscription(String transcriptionId) {
        client().delete().uri("/v2/recording_transcriptions/{id}", transcriptionId).retrieve();
    }

    /* Custom Storage Credentials */
    @Override
    public CustomStorageCredentialDto getCredentials(String connectionId) {
        return client().get().uri("/v2/custom_storage_credentials/{id}", connectionId)
                .retrieve().body(CustomStorageCredentialDto.class);
    }

    @Override
    public CustomStorageCredentialDto createCredentials(String connectionId, CustomStorageCredentialDto body) {
        return client().post().uri("/v2/custom_storage_credentials/{id}", connectionId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve().body(CustomStorageCredentialDto.class);
    }

    @Override
    public CustomStorageCredentialDto updateCredentials(String connectionId, CustomStorageCredentialDto body) {
        return client().put().uri("/v2/custom_storage_credentials/{id}", connectionId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve().body(CustomStorageCredentialDto.class);
    }

    @Override
    public void deleteCredentials(String connectionId) {
        client().delete().uri("/v2/custom_storage_credentials/{id}", connectionId).retrieve();
    }
}
