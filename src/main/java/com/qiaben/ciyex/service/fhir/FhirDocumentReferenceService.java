package com.qiaben.ciyex.service.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.FhirConfig;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirDocumentReferenceService {

    private final RestClient restClient;
    private final FhirAuthService fhirAuthService;
    private final OrgIntegrationConfigProvider integrationConfigProvider;
    private final FhirContext fhirContext = FhirContext.forR4();
    private static final String BASE_URL = "/DocumentReference";

    public Bundle getDocumentReferences(String _id, String _lastUpdated, String patient, String type, String category, String date) {
        FhirConfig fhirConfig = null;
        UriComponentsBuilder builder = null;
        String url = null;
        try {
            fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            builder = UriComponentsBuilder.fromHttpUrl(fhirConfig.getApiUrl() + BASE_URL);

            if (_id != null && !_id.isEmpty()) builder.queryParam("_id", _id);
            if (_lastUpdated != null && !_lastUpdated.isEmpty()) builder.queryParam("_lastUpdated", _lastUpdated);
            if (patient != null && !patient.isEmpty()) builder.queryParam("patient", patient);
            if (type != null && !type.isEmpty()) builder.queryParam("type", type);
            if (category != null && !category.isEmpty()) builder.queryParam("category", category);
            if (date != null && !date.isEmpty()) builder.queryParam("date", date);

            url = builder.build(true).toUriString();
            log.info("[FhirDocumentReferenceService] Fetching DocumentReferences: clientId={}, url={}",
                    fhirConfig.getClientId(), url);

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirDocumentReferenceService] DocumentReference bundle response (first 400 chars): {}",
                    response != null ? response.substring(0, Math.min(response.length(), 400)) : "null");

            IParser parser = fhirContext.newJsonParser();
            Bundle bundle = parser.parseResource(Bundle.class, response);

            log.info("[FhirDocumentReferenceService] Parsed {} DocumentReference entries from bundle.", bundle.getEntry().size());
            return bundle;
        } catch (Exception e) {
            log.error("[FhirDocumentReferenceService] Error fetching DocumentReferences (clientId={}, url={}): {}",
                    fhirConfig != null ? fhirConfig.getClientId() : null,
                    url, e.getMessage(), e);
            return new Bundle(); // Return an empty bundle in case of error
        }
    }

    public DocumentReference generateDocumentReference(String patient, String start, String end, String type) {
        FhirConfig fhirConfig = null;
        String url = null;
        try {
            fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            url = UriComponentsBuilder
                    .fromHttpUrl(fhirConfig.getApiUrl() + BASE_URL + "/$docref")
                    .queryParam("patient", patient)
                    .queryParam("start", start)
                    .queryParam("end", end)
                    .queryParam("type", type)
                    .build(true)
                    .toUriString();

            log.info("[FhirDocumentReferenceService] Generating DocumentReference: clientId={}, url={}, patient={}, type={}",
                    fhirConfig.getClientId(), url, patient, type);

            String response = restClient
                    .post()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirDocumentReferenceService] DocumentReference $docref response (first 400 chars): {}",
                    response != null ? response.substring(0, Math.min(response.length(), 400)) : "null");

            IParser parser = fhirContext.newJsonParser();
            DocumentReference docRef = parser.parseResource(DocumentReference.class, response);

            log.info("[FhirDocumentReferenceService] Generated DocumentReference ID: {}", docRef.getIdElement().getIdPart());
            return docRef;
        } catch (Exception e) {
            log.error("[FhirDocumentReferenceService] Error generating DocumentReference (clientId={}, url={}): {}",
                    fhirConfig != null ? fhirConfig.getClientId() : null,
                    url, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public DocumentReference getDocumentReference(String uuid) {
        FhirConfig fhirConfig = null;
        String url = null;
        try {
            fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            url = fhirConfig.getApiUrl() + BASE_URL + "/" + uuid;

            log.info("[FhirDocumentReferenceService] Fetching DocumentReference by UUID: clientId={}, url={}, uuid={}",
                    fhirConfig.getClientId(), url, uuid);

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirDocumentReferenceService] Single DocumentReference response (first 400 chars): {}",
                    response != null ? response.substring(0, Math.min(response.length(), 400)) : "null");

            IParser parser = fhirContext.newJsonParser();
            DocumentReference docRef = parser.parseResource(DocumentReference.class, response);

            log.info("[FhirDocumentReferenceService] Successfully parsed DocumentReference with UUID: {}", uuid);
            return docRef;
        } catch (Exception e) {
            log.error("[FhirDocumentReferenceService] Error fetching DocumentReference by UUID (clientId={}, url={}, uuid={}): {}",
                    fhirConfig != null ? fhirConfig.getClientId() : null,
                    url, uuid, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
