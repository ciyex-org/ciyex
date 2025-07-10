package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.FhirBinaryResponseDTO;
import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class FhirBinaryService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private OpenEmrFhirProperties openEmrFhirProperties;

    private static final String BINARY_URL = "/fhir/Binary/{id}";

    public FhirBinaryResponseDTO getBinaryDocument(String id) {
        String url = openEmrFhirProperties.getBaseUrl() + BINARY_URL;
        String authHeader = "Bearer " + openEmrFhirProperties.getToken();
        return restTemplate.getForObject(url, FhirBinaryResponseDTO.class, id, authHeader);
    }
}
