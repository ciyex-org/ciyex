package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.RecordingTranscriptionDto;
import com.qiaben.ciyex.dto.telnyx.RecordingTranscriptionListResponseDto;
import com.qiaben.ciyex.service.telnyx.RecordingTranscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/recording-transcription")
@RequiredArgsConstructor
public class RecordingTranscriptionController {

    private final RecordingTranscriptionService service;

    @GetMapping("/{accountSid}")
    public RecordingTranscriptionListResponseDto list(
            @PathVariable String accountSid,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String pageToken
    ) {
        return service.list(accountSid, pageSize != null ? pageSize : 10, pageToken);
    }

    @GetMapping("/{accountSid}/{transcriptionSid}")
    public RecordingTranscriptionDto get(
            @PathVariable String accountSid,
            @PathVariable String transcriptionSid
    ) {
        return service.get(accountSid, transcriptionSid);
    }

    @DeleteMapping("/{accountSid}/{transcriptionSid}")
    public void delete(
            @PathVariable String accountSid,
            @PathVariable String transcriptionSid
    ) {
        service.delete(accountSid, transcriptionSid);
    }
}
