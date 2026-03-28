package org.ciyex.ehr.fhir;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Manages FHIR partitions on the HAPI FHIR server.
 * Creates partitions via the $partition-management-create-partition operation.
 */
@Service
@Slf4j
public class FhirPartitionService {

    private final String fhirServerUrl;
    private final RestTemplate restTemplate = new RestTemplate();

    public FhirPartitionService(@Value("${fhir.server.url}") String fhirServerUrl) {
        this.fhirServerUrl = fhirServerUrl.endsWith("/")
                ? fhirServerUrl.substring(0, fhirServerUrl.length() - 1)
                : fhirServerUrl;
    }

    /**
     * Creates a new partition on the HAPI FHIR server for the given org alias.
     * Uses the $partition-management-create-partition operation on the DEFAULT partition.
     */
    public void createPartition(String orgAlias, String description) {
        String url = fhirServerUrl + "/DEFAULT/$partition-management-create-partition";

        // Get next partition ID
        int partitionId = getNextPartitionId();

        Map<String, Object> body = Map.of(
                "resourceType", "Parameters",
                "parameter", List.of(
                        Map.of("name", "id", "valueInteger", partitionId),
                        Map.of("name", "name", "valueCode", orgAlias),
                        Map.of("name", "description", "valueString", description != null ? description : orgAlias)
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            log.info("Created FHIR partition '{}' (id={}) - status: {}", orgAlias, partitionId, response.getStatusCode());
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                log.info("FHIR partition '{}' already exists", orgAlias);
            } else {
                throw new RuntimeException("Failed to create FHIR partition '" + orgAlias + "': " + e.getMessage(), e);
            }
        }
    }

    private int getNextPartitionId() {
        String url = fhirServerUrl + "/DEFAULT/$partition-management-list-partitions";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

            if (response.getBody() != null) {
                List<Map<String, Object>> parameters = (List<Map<String, Object>>) response.getBody().get("parameter");
                if (parameters != null) {
                    int maxId = 0;
                    for (Map<String, Object> param : parameters) {
                        List<Map<String, Object>> parts = (List<Map<String, Object>>) param.get("part");
                        if (parts != null) {
                            for (Map<String, Object> part : parts) {
                                if ("id".equals(part.get("name")) && part.get("valueInteger") != null) {
                                    int id = ((Number) part.get("valueInteger")).intValue();
                                    maxId = Math.max(maxId, id);
                                }
                            }
                        }
                    }
                    return maxId + 1;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to list FHIR partitions, using timestamp-based ID: {}", e.getMessage());
        }
        // Fallback: use timestamp-based ID (unlikely to collide)
        return (int) (System.currentTimeMillis() % 100000) + 100;
    }
}
