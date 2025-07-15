package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.*;
import com.qiaben.ciyex.service.telnyx.TeXmlApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/texml/applications")
@RequiredArgsConstructor
public class TeXmlApplicationController {

    private final TeXmlApplicationService applicationService;

    @GetMapping
    public ResponseEntity<TeXmlApplicationListResponseDto> listApplications(
            @RequestParam Map<String, String> queryParams) {
        TeXmlApplicationListResponseDto response = applicationService.listApplications(queryParams);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<TeXmlApplicationCreateResponseDto> createApplication(
            @RequestBody TeXmlApplicationCreateRequestDto request) {
        TeXmlApplicationCreateResponseDto response = applicationService.createApplication(request);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeXmlApplicationCreateResponseDto> getApplicationById(@PathVariable Long id) {
        TeXmlApplicationCreateResponseDto response = applicationService.getApplicationById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TeXmlApplicationCreateResponseDto> updateApplication(
            @PathVariable Long id,
            @RequestBody TeXmlApplicationUpdateRequestDto request) {
        TeXmlApplicationCreateResponseDto response = applicationService.updateApplication(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<TeXmlApplicationCreateResponseDto> deleteApplication(@PathVariable Long id) {
        TeXmlApplicationCreateResponseDto response = applicationService.deleteApplication(id);
        return ResponseEntity.ok(response);
    }
}
