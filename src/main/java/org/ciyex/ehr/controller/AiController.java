package org.ciyex.ehr.controller;

import org.ciyex.ehr.service.ai.AiGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/ai")
@Validated
public class AiController {

    private final AiGateway gateway;

    public AiController(AiGateway gateway) {
        this.gateway = gateway;
    }

    public record CompletionRequest(@NotBlank String prompt) {}
    public record CompletionResponse(String response) {}

    @PostMapping("/complete")
    public ResponseEntity<CompletionResponse> complete(@RequestBody CompletionRequest req) {
        String response = gateway.generateCompletion(req.prompt());
        return ResponseEntity.ok(new CompletionResponse(response));
    }
}