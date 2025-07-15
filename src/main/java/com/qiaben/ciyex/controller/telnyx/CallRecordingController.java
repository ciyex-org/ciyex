package com.qiaben.ciyex.controller.telnyx;


import com.qiaben.ciyex.dto.telnyx.*;
import com.qiaben.ciyex.service.telnyx.CallRecordingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/telnyx/recordings")
@RequiredArgsConstructor
public class CallRecordingController {

    private final CallRecordingService service;

    /* Recordings */
    @GetMapping
    public RecordingListResponseDto list(@RequestParam(value = "query", required = false) String query) {
        return service.listRecordings(query);
    }

    @GetMapping("/{id}")
    public RecordingDto get(@PathVariable String id) {
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
    public RecordingTranscriptionListResponseDto listTranscriptions() {
        return service.listTranscriptions();
    }

    @GetMapping("/transcriptions/{id}")
    public RecordingTranscriptionDto getTranscription(@PathVariable("id") String transcriptionId) {
        return service.getTranscription(transcriptionId);
    }

    @DeleteMapping("/transcriptions/{id}")
    public ResponseEntity<Void> deleteTranscription(@PathVariable("id") String transcriptionId) {
        service.deleteTranscription(transcriptionId);
        return ResponseEntity.noContent().build();
    }

    /* Credentials */
    @GetMapping("/credentials/{connectionId}")
    public CustomStorageCredentialDto getCredentials(@PathVariable String connectionId) {
        return service.getCredentials(connectionId);
    }

    @PostMapping("/credentials/{connectionId}")
    public CustomStorageCredentialDto createCredentials(@PathVariable String connectionId,
                                                        @RequestBody CustomStorageCredentialDto body) {
        return service.createCredentials(connectionId, body);
    }

    @PutMapping("/credentials/{connectionId}")
    public CustomStorageCredentialDto updateCredentials(@PathVariable String connectionId,
                                                        @RequestBody CustomStorageCredentialDto body) {
        return service.updateCredentials(connectionId, body);
    }

    @DeleteMapping("/credentials/{connectionId}")
    public ResponseEntity<Void> deleteCredentials(@PathVariable String connectionId) {
        service.deleteCredentials(connectionId);
        return ResponseEntity.noContent().build();
    }
}