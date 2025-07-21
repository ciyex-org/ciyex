package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxMediaDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxMediaListResponse;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxMediaUploadRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TelnyxMediaService {

    private final TelnyxProperties properties;
    private final RestClient restClient;

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(properties.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    public TelnyxMediaListResponse listMedia(String contentTypeFilter) {
        String url = UriComponentsBuilder
                .fromHttpUrl(properties.getApiBaseUrl() + "/media")
                .queryParam("filter[content_type][]", contentTypeFilter)
                .toUriString();

        return restClient.get()
                .uri(url)
                .headers(httpHeaders -> httpHeaders.addAll(headers()))
                .retrieve()
                .body(TelnyxMediaListResponse.class);
    }

    public TelnyxMediaDTO uploadMedia(TelnyxMediaUploadRequest request) {
        return restClient.post()
                .uri(properties.getApiBaseUrl() + "/media")
                .headers(httpHeaders -> httpHeaders.addAll(headers()))
                .body(request)
                .retrieve()
                .body(TelnyxMediaDTO.class);
    }

    public TelnyxMediaDTO retrieveMedia(String mediaName) {
        return restClient.get()
                .uri(properties.getApiBaseUrl() + "/media/" + mediaName)
                .headers(httpHeaders -> httpHeaders.addAll(headers()))
                .retrieve()
                .body(TelnyxMediaDTO.class);
    }

    public TelnyxMediaDTO updateMedia(String mediaName, TelnyxMediaUploadRequest request) {
        return restClient.put()
                .uri(properties.getApiBaseUrl() + "/media/" + mediaName)
                .headers(httpHeaders -> httpHeaders.addAll(headers()))
                .body(request)
                .retrieve()
                .body(TelnyxMediaDTO.class);
    }

    public void deleteMedia(String mediaName) {
        restClient.delete()
                .uri(properties.getApiBaseUrl() + "/media/" + mediaName)
                .headers(httpHeaders -> httpHeaders.addAll(headers()))
                .retrieve();
    }

    public byte[] downloadMedia(String mediaName) {
        return restClient.get()
                .uri(properties.getApiBaseUrl() + "/media/" + mediaName + "/download")
                .headers(httpHeaders -> httpHeaders.addAll(headers()))
                .retrieve()
                .body(byte[].class);
    }
}