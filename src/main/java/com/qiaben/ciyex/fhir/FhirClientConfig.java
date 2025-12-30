package com.qiaben.ciyex.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FhirClientConfig {

    @Value("${fhir.server.url}")
    private String fhirServerUrl;

    @Value("${fhir.client.socket-timeout:60000}")
    private int socketTimeout;

    @Value("${fhir.client.connect-timeout:10000}")
    private int connectTimeout;

    @Bean
    public IGenericClient fhirClient(FhirContext fhirContext) {
        fhirContext.getRestfulClientFactory().setSocketTimeout(socketTimeout);
        fhirContext.getRestfulClientFactory().setConnectTimeout(connectTimeout);
        fhirContext.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        
        return fhirContext.newRestfulGenericClient(fhirServerUrl);
    }
}
