package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.FhirDeviceDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FhirDeviceService {

    private final RestTemplate restTemplate;

    private final String baseUrl = "https://localhost:9300/apis/default/fhir/Device"; // Replace with actual URL

    public List<FhirDeviceDTO> getDevices(String _id, String _lastUpdated, String patient) {
        String url = baseUrl + "?_id=" + _id + "&_lastUpdated=" + _lastUpdated + "&patient=" + patient;
        return restTemplate.getForObject(url, List.class);
    }

    public FhirDeviceDTO getDeviceById(String uuid) {
        String url = baseUrl + "/" + uuid;
        return restTemplate.getForObject(url, FhirDeviceDTO.class);
    }
}

