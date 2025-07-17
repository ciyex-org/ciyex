package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxCallControlApplicationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TelnyxCallControlApplicationService {

    private final TelnyxProperties properties;          // inject from application.yml
    private final RestClient restClient = RestClient.create();
    private static final String BASE = "/v2/call_control_applications";


    public Object list(Map<String, String> params) {
        var builder = UriComponentsBuilder
                .fromHttpUrl(properties.getApiBaseUrl() + BASE);
        params.forEach(builder::queryParam);

        return restClient.get()
                .uri(builder.toUriString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .retrieve()
                .body(Object.class);
    }


    public Object create(TelnyxCallControlApplicationDTO dto) {
        return restClient.post()
                .uri(properties.getApiBaseUrl() + BASE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .body(Object.class);
    }


    public Object retrieve(Long id) {
        return restClient.get()
                .uri(properties.getApiBaseUrl() + BASE + "/" + id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .retrieve()
                .body(Object.class);
    }


    public Object update(Long id, TelnyxCallControlApplicationDTO dto) {
        return restClient.patch()
                .uri(properties.getApiBaseUrl() + BASE + "/" + id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .body(Object.class);
    }

    public Object delete(Long id) {
        return restClient.delete()
                .uri(properties.getApiBaseUrl() + BASE + "/" + id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .retrieve()
                .body(Object.class);
    }
}
