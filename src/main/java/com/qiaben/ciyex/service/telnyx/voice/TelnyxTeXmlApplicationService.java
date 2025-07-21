package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTeXmlApplicationCreateRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTeXmlApplicationCreateResponseDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTeXmlApplicationListResponseDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTeXmlApplicationUpdateRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TelnyxTeXmlApplicationService {

    private final RestClient restClient;
    private final TelnyxProperties telnyxProperties;

    public TelnyxTeXmlApplicationListResponseDto listApplications(Map<String, String> queryParams) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/texml_applications";
        return restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(url);
                    queryParams.forEach(uriBuilder::queryParam);
                    return uriBuilder.build();
                })
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxTeXmlApplicationListResponseDto.class);
    }

    public TelnyxTeXmlApplicationCreateResponseDto createApplication(TelnyxTeXmlApplicationCreateRequestDto request) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/texml_applications";
        return restClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .body(request)
                .retrieve()
                .body(TelnyxTeXmlApplicationCreateResponseDto.class);
    }

    public TelnyxTeXmlApplicationCreateResponseDto getApplicationById(Long id) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/texml_applications/{id}";
        return restClient.get()
                .uri(url, id)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxTeXmlApplicationCreateResponseDto.class);
    }

    public TelnyxTeXmlApplicationCreateResponseDto updateApplication(Long id, TelnyxTeXmlApplicationUpdateRequestDto request) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/texml_applications/{id}";
        return restClient.patch()
                .uri(url, id)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .body(request)
                .retrieve()
                .body(TelnyxTeXmlApplicationCreateResponseDto.class);
    }

    public TelnyxTeXmlApplicationCreateResponseDto deleteApplication(Long id) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/texml_applications/{id}";
        return restClient.delete()
                .uri(url, id)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxTeXmlApplicationCreateResponseDto.class);
    }
}
