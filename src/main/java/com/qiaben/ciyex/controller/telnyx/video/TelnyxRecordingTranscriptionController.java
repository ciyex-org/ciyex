package com.qiaben.ciyex.controller.telnyx.video;

import com.qiaben.ciyex.dto.telnyx.video.TelnyxRecordingTranscriptionDto;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxRecordingTranscriptionListResponseDto;
import com.qiaben.ciyex.service.telnyx.video.TelnyxRecordingTranscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/recording-transcription")
@RequiredArgsConstructor
public class TelnyxRecordingTranscriptionController {

    private final TelnyxRecordingTranscriptionService service;

    @GetMapping("/{accountSid}")
    public TelnyxRecordingTranscriptionListResponseDto list(
            @PathVariable String accountSid,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String pageToken
    ) {
        return service.list(accountSid, pageSize != null ? pageSize : 10, pageToken);
    }

    @GetMapping("/{accountSid}/{transcriptionSid}")
    public TelnyxRecordingTranscriptionDto get(
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
