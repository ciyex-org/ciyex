package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TeXmlApplicationService {

    private final RestClient restClient;
    private final TelnyxProperties telnyxProperties;

    public TeXmlApplicationListResponseDto listApplications(Map<String, String> queryParams) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/texml_applications";
        return restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(url);
                    queryParams.forEach(uriBuilder::queryParam);
                    return uriBuilder.build();
                })
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TeXmlApplicationListResponseDto.class);
    }

    public TeXmlApplicationCreateResponseDto createApplication(TeXmlApplicationCreateRequestDto request) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/texml_applications";
        return restClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .body(request)
                .retrieve()
                .body(TeXmlApplicationCreateResponseDto.class);
    }

    public TeXmlApplicationCreateResponseDto getApplicationById(Long id) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/texml_applications/{id}";
        return restClient.get()
                .uri(url, id)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TeXmlApplicationCreateResponseDto.class);
    }

    public TeXmlApplicationCreateResponseDto updateApplication(Long id, TeXmlApplicationUpdateRequestDto request) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/texml_applications/{id}";
        return restClient.patch()
                .uri(url, id)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .body(request)
                .retrieve()
                .body(TeXmlApplicationCreateResponseDto.class);
    }

    public TeXmlApplicationCreateResponseDto deleteApplication(Long id) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/texml_applications/{id}";
        return restClient.delete()
                .uri(url, id)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TeXmlApplicationCreateResponseDto.class);
    }
}
