package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.QualifyByUsecaseResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class QualifyByUsecaseService {

    private final TelnyxProperties properties;
    private final RestClient restClient = RestClient.builder().build();

    public QualifyByUsecaseResponseDTO qualify(String brandId, String usecase) {
        String url = String.format("%s/v2/10dlc/campaignBuilder/brand/%s/usecase/%s",
                properties.getApiBaseUrl(), brandId, usecase);

        return restClient.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(QualifyByUsecaseResponseDTO.class);
    }
}
