package org.ciyex.ehr.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

/**
 * Proxy controller that forwards code lookup requests to the ciyex-codes service.
 * The codes service holds the full medical code database (ICD-10, CPT, HCPCS, etc.).
 */
@RestController
@RequestMapping("/api/codes-proxy")
@Slf4j
public class CodesProxyController {

    private final RestClient restClient;
    private final String codesServiceUrl;

    public CodesProxyController(
            RestClient restClient,
            @Value("${codes.service.url:https://codes-api.apps-dev.us-east.in.hinisoft.com}") String codesServiceUrl) {
        this.restClient = restClient;
        this.codesServiceUrl = codesServiceUrl.replaceAll("/+$", "");
    }

    @GetMapping("/{system}/search")
    public ResponseEntity<String> searchCodes(
            @PathVariable String system,
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            HttpServletRequest request) {
        try {
            String url = String.format("%s/api/codes/%s/search?q=%s&page=%d&size=%d",
                    codesServiceUrl, system, q, page, size);
            log.debug("Proxying codes search to: {}", url);

            String body = restClient.get()
                    .uri(url)
                    .headers(h -> forwardAuth(request, h))
                    .retrieve()
                    .body(String.class);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);
        } catch (Exception e) {
            log.error("Failed to proxy codes search: system={} q={}", system, q, e);
            return ResponseEntity.ok("{\"content\":[],\"totalElements\":0}");
        }
    }

    @GetMapping("/{system}/{code}")
    public ResponseEntity<String> getCode(
            @PathVariable String system,
            @PathVariable String code,
            HttpServletRequest request) {
        try {
            String url = String.format("%s/api/codes/%s/%s", codesServiceUrl, system, code);
            String body = restClient.get()
                    .uri(url)
                    .headers(h -> forwardAuth(request, h))
                    .retrieve()
                    .body(String.class);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);
        } catch (Exception e) {
            log.error("Failed to proxy code lookup: system={} code={}", system, code, e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{system}/categories")
    public ResponseEntity<String> getCategories(
            @PathVariable String system,
            HttpServletRequest request) {
        try {
            String url = String.format("%s/api/codes/%s/categories", codesServiceUrl, system);
            String body = restClient.get()
                    .uri(url)
                    .headers(h -> forwardAuth(request, h))
                    .retrieve()
                    .body(String.class);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);
        } catch (Exception e) {
            log.error("Failed to proxy categories: system={}", system, e);
            return ResponseEntity.ok("[]");
        }
    }

    private void forwardAuth(HttpServletRequest request, org.springframework.http.HttpHeaders headers) {
        String auth = request.getHeader("Authorization");
        if (auth != null) {
            headers.set("Authorization", auth);
        }
    }
}
