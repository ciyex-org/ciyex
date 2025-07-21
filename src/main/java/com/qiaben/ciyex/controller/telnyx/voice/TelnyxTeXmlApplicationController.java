package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTeXmlApplicationCreateRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTeXmlApplicationCreateResponseDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTeXmlApplicationListResponseDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTeXmlApplicationUpdateRequestDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxTeXmlApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/texml/applications")
@RequiredArgsConstructor
public class TelnyxTeXmlApplicationController {

    private final TelnyxTeXmlApplicationService applicationService;

    @GetMapping
    public ResponseEntity<TelnyxTeXmlApplicationListResponseDto> listApplications(
            @RequestParam Map<String, String> queryParams) {
        TelnyxTeXmlApplicationListResponseDto response = applicationService.listApplications(queryParams);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<TelnyxTeXmlApplicationCreateResponseDto> createApplication(
            @RequestBody TelnyxTeXmlApplicationCreateRequestDto request) {
        TelnyxTeXmlApplicationCreateResponseDto response = applicationService.createApplication(request);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TelnyxTeXmlApplicationCreateResponseDto> getApplicationById(@PathVariable Long id) {
        TelnyxTeXmlApplicationCreateResponseDto response = applicationService.getApplicationById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TelnyxTeXmlApplicationCreateResponseDto> updateApplication(
            @PathVariable Long id,
            @RequestBody TelnyxTeXmlApplicationUpdateRequestDto request) {
        TelnyxTeXmlApplicationCreateResponseDto response = applicationService.updateApplication(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<TelnyxTeXmlApplicationCreateResponseDto> deleteApplication(@PathVariable Long id) {
        TelnyxTeXmlApplicationCreateResponseDto response = applicationService.deleteApplication(id);
        return ResponseEntity.ok(response);
    }
}
