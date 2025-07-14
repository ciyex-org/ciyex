package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.TeXmlCallRecordingListResponseDto;
import com.qiaben.ciyex.dto.telnyx.TeXmlCallRecordingResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TeXmlAccountRecordingService {

    private final RestClient restClient;
    private final TelnyxProperties telnyxProperties;

    public TeXmlCallRecordingListResponseDto getAllRecordings(String accountSid, Map<String, String> queryParams) {
        String baseUrl = telnyxProperties.getApiBaseUrl()
                + "/v2/texml/Accounts/" + accountSid + "/Recordings.json";

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);
        Optional.ofNullable(queryParams).ifPresent(q -> q.forEach(builder::queryParam));

        return restClient.get()
                .uri(builder.build().toUri())
                .header("Authorization", authHeader())
                .retrieve()
                .body(TeXmlCallRecordingListResponseDto.class);
    }

    public TeXmlCallRecordingResponseDto getRecordingById(String accountSid, String recordingSid) {
        String url = telnyxProperties.getApiBaseUrl()
                + "/v2/texml/Accounts/{accountSid}/Recordings/{recordingSid}.json";

        try {
            return restClient.get()
                    .uri(url, accountSid, recordingSid)
                    .header("Authorization", authHeader())
                    .retrieve()
                    .body(TeXmlCallRecordingResponseDto.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new RuntimeException("Recording not found: " + recordingSid);
        }
    }

    public void deleteRecordingById(String accountSid, String recordingSid) {
        String url = telnyxProperties.getApiBaseUrl()
                + "/v2/texml/Accounts/{accountSid}/Recordings/{recordingSid}.json";

        try {
            restClient.delete()
                    .uri(url, accountSid, recordingSid)
                    .header("Authorization", authHeader())
                    .retrieve();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new RuntimeException("Recording not found: " + recordingSid);
        }
    }

    public TeXmlCallRecordingListResponseDto getConferenceRecordings(
            String accountSid,
            String conferenceSid
    ) {
        String url = telnyxProperties.getApiBaseUrl()
                + "/v2/texml/Accounts/{accountSid}/Conferences/{conferenceSid}/Recordings.json";

        return restClient.get()
                .uri(url, accountSid, conferenceSid)
                .header("Authorization", authHeader())
                .retrieve()
                .body(TeXmlCallRecordingListResponseDto.class);
    }

    private String authHeader() {
        return "Bearer " + telnyxProperties.getApiKey();
    }
}
