package com.qiaben.ciyex.config;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiaben.ciyex.dto.core.integration.FhirConfig;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.RequestContext;
import com.qiaben.ciyex.storage.ExternalOrgStorage;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.NoOpExternalStorage;
import com.qiaben.ciyex.storage.StorageType;
import com.qiaben.ciyex.storage.fhir.FhirExternalOrgStorage;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    @SuppressWarnings("unchecked")
    public Map<String, List<ExternalStorage<?>>> storageImplementations(ApplicationContext applicationContext) {
        Map<String, List<ExternalStorage<?>>> storageMap = new HashMap<>();

        // Get all beans implementing ExternalStorage
        Map<String, Object> storageBeans = applicationContext.getBeansWithAnnotation(StorageType.class);

        for (Map.Entry<String, Object> entry : storageBeans.entrySet()) {
            Object bean = entry.getValue();
            if (bean instanceof ExternalStorage<?>) {
                ExternalStorage<?> storage = (ExternalStorage<?>) bean;
                StorageType storageTypeAnnotation = AnnotationUtils.findAnnotation(bean.getClass(), StorageType.class);
                if (storageTypeAnnotation != null) {
                    String storageType = storageTypeAnnotation.value();
                    storageMap.computeIfAbsent(storageType, k -> new ArrayList<>()).add(storage);
                } else {
                    // Fallback to bean name convention if annotation is missing
                    String beanName = entry.getKey();
                    String derivedType = deriveStorageType(beanName);
                    if (!"unknown".equals(derivedType)) {
                        storageMap.computeIfAbsent(derivedType, k -> new ArrayList<>()).add(storage);
                    }
                }
            }
        }

        return storageMap;
    }

    private String deriveStorageType(String beanName) {
        // Convention: Extract "fhir" from "fhirExternalOrgStorage" or "fhirExternalLocationStorage"
        if (beanName.startsWith("fhir")) {
            return "fhir";
        }
        // Add more conditions for other storage types (e.g., "noOp" for NoOpExternalStorage)
        return "unknown"; // Default fallback
    }
}