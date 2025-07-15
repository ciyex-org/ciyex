package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.fhir.FhirDocumentReferenceDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FhirDocumentReferenceService {

    private final RestClient restClient;
    private final OpenEmrFhirProperties openEmrFhirProperties;
    private static final String BASE_URL = "/fhir/DocumentReference";
    private final OpenEmrAuthService openEmrAuthService;

    public List<FhirDocumentReferenceDTO> getDocumentReferences(String _id, String _lastUpdated, String patient, String type, String category, String date) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(openEmrFhirProperties.getBaseUrl() + BASE_URL);

            if (_id != null) builder.queryParam("_id", _id);
            if (_lastUpdated != null) builder.queryParam("_lastUpdated", _lastUpdated);
            if (patient != null) builder.queryParam("patient", patient);
            if (type != null) builder.queryParam("type", type);
            if (category != null) builder.queryParam("category", category);
            if (date != null) builder.queryParam("date", date);

            return restClient
                    .get()
                    .uri(builder.build(true).toUri())
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<FhirDocumentReferenceDTO>>() {
                    });
        }catch (Exception e) {
            e.printStackTrace();
            return List.of(); // Return an empty list in case of error
        }
    }

    public FhirDocumentReferenceDTO generateDocumentReference(String patient, String start, String end, String type) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(openEmrFhirProperties.getBaseUrl() + BASE_URL + "/$docref")
                    .queryParam("patient", patient)
                    .queryParam("start", start)
                    .queryParam("end", end)
                    .queryParam("type", type)
                    .build(true)
                    .toUriString();

            return restClient
                    .post()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(FhirDocumentReferenceDTO.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public FhirDocumentReferenceDTO getDocumentReference(String uuid) {
        try {
            String url = openEmrFhirProperties.getBaseUrl() + BASE_URL + "/" + uuid;

            return restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(FhirDocumentReferenceDTO.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
