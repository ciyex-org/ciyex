package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.CallControlApplicationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CallControlApplicationService {

    private final TelnyxProperties properties;          // inject from application.yml
    private final RestClient restClient = RestClient.create();
    private static final String BASE = "/v2/call_control_applications";

    /* ---------- LIST ---------- */
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

    /* ---------- CREATE ---------- */
    public Object create(CallControlApplicationDTO dto) {
        return restClient.post()
                .uri(properties.getApiBaseUrl() + BASE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .body(Object.class);
    }

    /* ---------- RETRIEVE ---------- */
    public Object retrieve(Long id) {
        return restClient.get()
                .uri(properties.getApiBaseUrl() + BASE + "/" + id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .retrieve()
                .body(Object.class);
    }

    /* ---------- UPDATE ---------- */
    public Object update(Long id, CallControlApplicationDTO dto) {
        return restClient.patch()
                .uri(properties.getApiBaseUrl() + BASE + "/" + id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .body(Object.class);
    }

    /* ---------- DELETE ---------- */
    public Object delete(Long id) {
        return restClient.delete()
                .uri(properties.getApiBaseUrl() + BASE + "/" + id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .retrieve()
                .body(Object.class);
    }
}
