package com.qiaben.ciyex.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiaben.ciyex.dto.core.integration.FhirConfig;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.RequestContext;
import com.qiaben.ciyex.storage.ExternalOrgStorage;
import com.qiaben.ciyex.storage.NoOpExternalOrgStorage;
import com.qiaben.ciyex.storage.fhir.FhirExternalOrgStorage;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;


@Configuration
public class CiyexAppConfig {
    // Define RestTemplate as a Spring Bean
    @Bean
    public RestClient restClient(OrgIntegrationConfigProvider integrationConfigProvider) {
        return RestClient.builder()
                .requestInterceptor((request, body, execution) -> {
                    FhirConfig fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
                    String fhirApiUrl = fhirConfig.getApiUrl(); // this is the actual String you need

                    String reqUrl = request.getURI().toString();
                    if (reqUrl.startsWith(fhirApiUrl)) {
                        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
                        if (orgId == null) {
                            throw new IllegalStateException("No orgId found in RequestContext for FHIR request to: " + reqUrl);
                        }
                        URI uriWithOrgId = UriComponentsBuilder.fromUri(request.getURI())
                                .queryParam("_tag", orgId)
                                .build(true)
                                .toUri();
                        HttpRequestWrapper wrapper = new HttpRequestWrapper(request) {
                            @Override
                            public URI getURI() {
                                return uriWithOrgId;
                            }
                        };
                        return execution.execute(wrapper, body);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Only include non-empty fields (null, empty strings, empty collections will be skipped)
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        return mapper;
    }

    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4(); // Provide a shared FhirContext bean
    }

    @Bean
    public Map<String, ExternalOrgStorage> storageImplementations(FhirExternalOrgStorage fhirExternalOrgStorage) {
        return Map.of(
                "fhir", fhirExternalOrgStorage,
                "noOp", new NoOpExternalOrgStorage()
                // Add more implementations, e.g., "practice_db", practiceDbExternalOrgStorage
        );
    }
}
