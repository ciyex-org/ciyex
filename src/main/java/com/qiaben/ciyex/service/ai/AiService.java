package com.qiaben.ciyex.service.ai;

public interface AiService {

    /**
     * Generates a completion based on the given prompt, using the organization's AI config.
     *
     * @param prompt The input prompt for the AI model
     * @return The generated response from the AI model
     */
    String generateCompletion(String prompt);
}