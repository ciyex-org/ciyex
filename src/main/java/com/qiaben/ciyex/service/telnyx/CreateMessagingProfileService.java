package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.dto.telnyx.CreateMessagingProfileRequestDto;
import com.qiaben.ciyex.dto.telnyx.CreateMessagingProfileResponseDto;
import com.qiaben.ciyex.config.TelnyxProperties;  // Correct import
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.sendgrid.SendGridProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class CreateMessagingProfileService {

    private final RestClient restClient;
    private final TelnyxProperties telnyxProperties;  // Use the correct class

    public CreateMessagingProfileResponseDto createMessagingProfile(CreateMessagingProfileRequestDto requestDto) {
        String url = telnyxProperties.getApiBaseUrl() + "/messaging_profiles";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        SendGridProperties telynxProperties = null;
        headers.setBearerAuth(telynxProperties.getApiKey());

        return restClient.post()
                .uri(url)
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .body(requestDto)
                .retrieve()
                .body(CreateMessagingProfileResponseDto.class);
    }
}
