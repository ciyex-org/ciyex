package com.qiaben.ciyex.service.telnyx.voice;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTeXmlCallRecordingListResponseDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTeXmlCallRecordingRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTeXmlCallRecordingResponseDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTeXmlUpdateCallRecordingRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TelnyxTeXmlCallRecordingService {

    private final RestClient restClient;
    private final TelnyxProperties telnyxProperties;
    private final ObjectMapper objectMapper;

    public TelnyxTeXmlCallRecordingResponseDto startCallRecording(
            String accountSid, String callSid, TelnyxTeXmlCallRecordingRequestDto body) {

        String url = telnyxProperties.getApiBaseUrl()
                + "/v2/texml/Accounts/{accountSid}/Calls/{callSid}/Recordings.json";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        Map<String, Object> map = objectMapper.convertValue(body, new TypeReference<>() {});
        map.forEach((k, v) -> { if (v != null) form.add(k, v.toString()); });

        return restClient.post()
                .uri(url, accountSid, callSid)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.valueOf("application/x-www-form-urlencoded"))
                .body(form)
                .retrieve()
                .body(TelnyxTeXmlCallRecordingResponseDto.class);
    }

    public TelnyxTeXmlCallRecordingListResponseDto getCallRecordings(
            String accountSid, String callSid) {

        String url = telnyxProperties.getApiBaseUrl()
                + "/v2/texml/Accounts/{accountSid}/Calls/{callSid}/Recordings.json";

        return restClient.get()
                .uri(url, accountSid, callSid)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxTeXmlCallRecordingListResponseDto.class);
    }


    public TelnyxTeXmlCallRecordingResponseDto updateCallRecording(
            String accountSid,
            String callSid,
            String recordingSid,
            TelnyxTeXmlUpdateCallRecordingRequestDto body) {

        String url = telnyxProperties.getApiBaseUrl()
                + "/v2/texml/Accounts/{accountSid}/Calls/{callSid}/Recordings/{recordingSid}.json";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        if (body.getStatus() != null) form.add("Status", body.getStatus());

        return restClient.post()
                .uri(url, accountSid, callSid, recordingSid)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.valueOf("application/x-www-form-urlencoded"))
                .body(form)
                .retrieve()
                .body(TelnyxTeXmlCallRecordingResponseDto.class);
    }
}
