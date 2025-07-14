package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.dto.fhir.FhirDeviceDTO;
import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FhirDeviceService {

    private final RestTemplate restTemplate;
    private final OpenEmrFhirProperties openEmrFhirProperties;

    public List<FhirDeviceDTO> getDevices(String _id, String _lastUpdated, String patient) {
        String url = openEmrFhirProperties.getBaseUrl() + "/fhir/Device?_id=" + _id + "&_lastUpdated=" + _lastUpdated + "&patient=" + patient;
        return restTemplate.getForObject(url, List.class);
    }

    public FhirDeviceDTO getDeviceById(String uuid) {
        String url = openEmrFhirProperties.getBaseUrl() + "/fhir/Device/" + uuid;
        return restTemplate.getForObject(url, FhirDeviceDTO.class);
    }
}
