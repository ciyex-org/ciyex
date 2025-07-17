package com.qiaben.ciyex.service.telnyx.voice;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TelnyxTeXmlConferenceService {

    private final RestClient restClient;
    private final TelnyxProperties telnyxProperties;



    public TelnyxTeXmlConferenceDto fetchConference(String accountSid, String conferenceSid) {
        String url = telnyxProperties.getApiBaseUrl()
                + "/v2/texml/Accounts/{accountSid}/Conferences/{conferenceSid}";
        return restClient.get()
                .uri(url, accountSid, conferenceSid)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxTeXmlConferenceDto.class);
    }

    public TelnyxTeXmlConferenceDto updateConference(String accountSid, String conferenceSid, TelnyxTeXmlConferenceUpdateRequestDto requestDto) {
        String url = telnyxProperties.getApiBaseUrl()
                + "/v2/texml/Accounts/{accountSid}/Conferences/{conferenceSid}";

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        if (requestDto.getStatus() != null) formData.add("Status", requestDto.getStatus());
        if (requestDto.getAnnounceUrl() != null) formData.add("AnnounceUrl", requestDto.getAnnounceUrl());
        if (requestDto.getAnnounceMethod() != null) formData.add("AnnounceMethod", requestDto.getAnnounceMethod());

        return restClient.post()
                .uri(url, accountSid, conferenceSid)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.valueOf("application/x-www-form-urlencoded"))
                .body(formData)
                .retrieve()
                .body(TelnyxTeXmlConferenceDto.class);
    }

    public TelnyxTeXmlConferenceListResponseDto listConferences(String accountSid, Map<String, String> filters) {
        String baseUri = telnyxProperties.getApiBaseUrl()
                + "/v2/texml/Accounts/{accountSid}/Conferences";

        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        filters.forEach(queryParams::add);

        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path(baseUri).queryParams(queryParams).build(accountSid))
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxTeXmlConferenceListResponseDto.class);
    }

    public TelnyxTeXmlRecordingListResponseDto listRecordings(String accountSid, String conferenceSid) {
        String url = telnyxProperties.getApiBaseUrl()
                + "/v2/texml/Accounts/{accountSid}/Conferences/{conferenceSid}/Recordings";

        return restClient.get()
                .uri(url, accountSid, conferenceSid)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxTeXmlRecordingListResponseDto.class);
    }


    public TelnyxTeXmlParticipantListResponseDto listParticipants(String accountSid, String conferenceSid) {
        String url = telnyxProperties.getApiBaseUrl()
                + "/v2/texml/Accounts/{accountSid}/Conferences/{conferenceSid}/Participants";

        return restClient.get()
                .uri(url, accountSid, conferenceSid)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxTeXmlParticipantListResponseDto.class);
    }

    public TelnyxTeXmlParticipantResponseDto getParticipant(String accountSid, String conferenceSid, String callSidOrLabel) {
        String url = telnyxProperties.getApiBaseUrl()
                + "/v2/texml/Accounts/{accountSid}/Conferences/{conferenceSid}/Participants/{id}";

        return restClient.get()
                .uri(url, accountSid, conferenceSid, callSidOrLabel)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxTeXmlParticipantResponseDto.class);
    }

    public TelnyxTeXmlParticipantResponseDto dialParticipant(String accountSid, String conferenceSid, TelnyxTeXmlDialParticipantRequestDto requestDto) {
        String url = telnyxProperties.getApiBaseUrl()
                + "/v2/texml/Accounts/{accountSid}/Conferences/{conferenceSid}/Participants";

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> requestMap = mapper.convertValue(requestDto, new TypeReference<>() {});
        requestMap.forEach((key, value) -> {
            if (value != null) formData.add(key, value.toString());
        });

        return restClient.post()
                .uri(url, accountSid, conferenceSid)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.valueOf("application/x-www-form-urlencoded"))
                .body(formData)
                .retrieve()
                .body(TelnyxTeXmlParticipantResponseDto.class);
    }

    public TelnyxTeXmlParticipantResponseDto updateParticipant(String accountSid, String conferenceSid, String callSidOrLabel, TelnyxTeXmlParticipantUpdateRequestDto requestDto) {
        String url = telnyxProperties.getApiBaseUrl()
                + "/v2/texml/Accounts/{accountSid}/Conferences/{conferenceSid}/Participants/{id}";

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> requestMap = mapper.convertValue(requestDto, new TypeReference<>() {});
        requestMap.forEach((key, value) -> {
            if (value != null) formData.add(key, value.toString());
        });

        return restClient.post()
                .uri(url, accountSid, conferenceSid, callSidOrLabel)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.valueOf("application/x-www-form-urlencoded"))
                .body(formData)
                .retrieve()
                .body(TelnyxTeXmlParticipantResponseDto.class);
    }

    public void deleteParticipant(String accountSid, String conferenceSid, String callSidOrLabel) {
        String url = telnyxProperties.getApiBaseUrl()
                + "/v2/texml/Accounts/{accountSid}/Conferences/{conferenceSid}/Participants/{id}";

        restClient.delete()
                .uri(url, accountSid, conferenceSid, callSidOrLabel)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .toBodilessEntity(); // 204 No Content
    }
}
