package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxStartStreamRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxStreamResponseDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxUpdateStreamRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxMediaStreamService {

    private final RestClient restClient;
    private final TelnyxProperties telnyxProperties;

    public TelnyxStreamResponseDto startStream(String accountSid, String callSid, TelnyxStartStreamRequestDto req) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        if (req.getStatusCallback() != null) form.add("StatusCallback", req.getStatusCallback());
        if (req.getStatusCallbackMethod() != null) form.add("StatusCallbackMethod", req.getStatusCallbackMethod());
        if (req.getTrack() != null) form.add("Track", req.getTrack());
        if (req.getName() != null) form.add("Name", req.getName());
        if (req.getBidirectionalMode() != null) form.add("BidirectionalMode", req.getBidirectionalMode());
        if (req.getBidirectionalCodec() != null) form.add("BidirectionalCodec", req.getBidirectionalCodec());
        if (req.getUrl() != null) form.add("Url", req.getUrl());

        return restClient.post()
                .uri(telnyxProperties.getApiBaseUrl() + "/v2/texml/Accounts/{accountSid}/Calls/{callSid}/Stream", accountSid, callSid)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TelnyxStreamResponseDto.class);
    }

    public TelnyxStreamResponseDto updateStream(String accountSid, String callSid, String streamSid, TelnyxUpdateStreamRequestDto req) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        if (req.getStatus() != null) form.add("Status", req.getStatus());

        return restClient.post()
                .uri(telnyxProperties.getApiBaseUrl() + "/v2/texml/Accounts/{accountSid}/Calls/{callSid}/Stream/{streamSid}", accountSid, callSid, streamSid)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TelnyxStreamResponseDto.class);
    }
}

