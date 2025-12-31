package com.qiaben.ciyex.fhir;

import org.springframework.context.annotation.Configuration;

/**
 * FHIR Configuration.
 * FhirContext bean is provided by CiyexAppConfig.
 * Client creation is handled by FhirClientService with URL path-based partitioning
 * (e.g., /fhir/sunrise-family-medicine/Patient).
 */
@Configuration
public class FhirClientConfig {
    // FhirContext bean already defined in CiyexAppConfig
}
