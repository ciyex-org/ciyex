package com.qiaben.ciyex.service.ai;

import org.springframework.stereotype.Service;

@Service
public class AiGateway {

    private final AiResolver resolver;

    public AiGateway(AiResolver resolver) {
        this.resolver = resolver;
    }

    public String generateCompletion(String prompt) {
        return resolver.resolve().generateCompletion(prompt);
    }
}