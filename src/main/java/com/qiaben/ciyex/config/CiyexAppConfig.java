package com.qiaben.ciyex.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.boot.web.client.RestClientBuilder;


@Configuration
public class CiyexAppConfig {
    @Bean
    public RestClient restClient(RestClientBuilder builder) {
        return builder.build();
    }
}
