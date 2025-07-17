package com.qiaben.ciyex.controller.telnyx.voice;


import com.qiaben.ciyex.dto.telnyx.video.TelnyxRecordingDto;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxRecordingListResponseDto;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxRecordingTranscriptionDto;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxRecordingTranscriptionListResponseDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxCustomStorageCredentialDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxCallRecordingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/telnyx/recordings")
@RequiredArgsConstructor
public class TelnyxCallRecordingController {

    private final TelnyxCallRecordingService service;

    /* Recordings */
    @GetMapping
    public TelnyxRecordingListResponseDto list(@RequestParam(value = "query", required = false) String query) {
        return service.listRecordings(query);
    }

    @GetMapping("/{id}")
    public TelnyxRecordingDto get(@PathVariable String id) {
        return service.getRecording(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.deleteRecording(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/batch")
    public ResponseEntity<Void> deleteBatch(@RequestBody List<String> ids) {
        service.deleteRecordingsBatch(ids);
        return ResponseEntity.noContent().build();
    }

    /* Transcriptions */
    @GetMapping("/transcriptions")
    public TelnyxRecordingTranscriptionListResponseDto listTranscriptions() {
        return service.listTranscriptions();
    }

    @GetMapping("/transcriptions/{id}")
    public TelnyxRecordingTranscriptionDto getTranscription(@PathVariable("id") String transcriptionId) {
        return service.getTranscription(transcriptionId);
    }

    @DeleteMapping("/transcriptions/{id}")
    public ResponseEntity<Void> deleteTranscription(@PathVariable("id") String transcriptionId) {
        service.deleteTranscription(transcriptionId);
        return ResponseEntity.noContent().build();
    }

    /* Credentials */
    @GetMapping("/credentials/{connectionId}")
    public TelnyxCustomStorageCredentialDto getCredentials(@PathVariable String connectionId) {
        return service.getCredentials(connectionId);
    }

    @PostMapping("/credentials/{connectionId}")
    public TelnyxCustomStorageCredentialDto createCredentials(@PathVariable String connectionId,
                                                              @RequestBody TelnyxCustomStorageCredentialDto body) {
        return service.createCredentials(connectionId, body);
    }

    @PutMapping("/credentials/{connectionId}")
    public TelnyxCustomStorageCredentialDto updateCredentials(@PathVariable String connectionId,
                                                              @RequestBody TelnyxCustomStorageCredentialDto body) {
        return service.updateCredentials(connectionId, body);
    }

    @DeleteMapping("/credentials/{connectionId}")
    public ResponseEntity<Void> deleteCredentials(@PathVariable String connectionId) {
        service.deleteCredentials(connectionId);
        return ResponseEntity.noContent().build();
    }
}