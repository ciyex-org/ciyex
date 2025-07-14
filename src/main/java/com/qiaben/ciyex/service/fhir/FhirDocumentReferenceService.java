package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.fhir.FhirDocumentReferenceDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class FhirDocumentReferenceService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private OpenEmrFhirProperties openEmrFhirProperties;

    private static final String BASE_URL = "/fhir/DocumentReference";

    public List<FhirDocumentReferenceDTO> getDocumentReferences(String _id, String _lastUpdated, String patient, String type, String category, String date) {
        // Call OpenEMR API using RestTemplate to get DocumentReference
        String url = openEmrFhirProperties.getBaseUrl() + BASE_URL + "?_id=" + _id + "&_lastUpdated=" + _lastUpdated + "&patient=" + patient + "&type=" + type + "&category=" + category + "&date=" + date;
        FhirDocumentReferenceDTO[] response = restTemplate.getForObject(url, FhirDocumentReferenceDTO[].class);
        return List.of(response);
    }

    public FhirDocumentReferenceDTO generateDocumentReference(String patient, String start, String end, String type) {
        String url = openEmrFhirProperties.getBaseUrl() + BASE_URL + "/$docref?patient=" + patient + "&start=" + start + "&end=" + end + "&type=" + type;
        return restTemplate.postForObject(url, null, FhirDocumentReferenceDTO.class);
    }

    public FhirDocumentReferenceDTO getDocumentReference(String uuid) {
        String url = openEmrFhirProperties.getBaseUrl() + BASE_URL + "/" + uuid;
        return restTemplate.getForObject(url, FhirDocumentReferenceDTO.class);
    }
}
