package com.qiaben.ciyex.service.telnyx.voice;   // ← must match folder

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxCallResponseDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxUpdateCallRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxUpdateCallService {

    private final RestClient restClient;
    private final TelnyxProperties telnyxProperties;

    public TelnyxCallResponseDto updateCall(String accountSid, String callSid, TelnyxUpdateCallRequestDto request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        if (request.getStatus() != null)               form.add("Status", request.getStatus());
        if (request.getUrl() != null)                  form.add("Url", request.getUrl());
        if (request.getMethod() != null)               form.add("Method", request.getMethod());
        if (request.getFallbackUrl() != null)          form.add("FallbackUrl", request.getFallbackUrl());
        if (request.getFallbackMethod() != null)       form.add("FallbackMethod", request.getFallbackMethod());
        if (request.getStatusCallback() != null)       form.add("StatusCallback", request.getStatusCallback());
        if (request.getStatusCallbackMethod() != null) form.add("StatusCallbackMethod", request.getStatusCallbackMethod());
        if (request.getTexml() != null)                form.add("Texml", request.getTexml());

        return restClient.post()
                .uri(telnyxProperties.getApiBaseUrl() +
                                "/v2/texml/Accounts/{accountSid}/Calls/{callSid}",
                        accountSid, callSid)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TelnyxCallResponseDto.class);
    }
}
