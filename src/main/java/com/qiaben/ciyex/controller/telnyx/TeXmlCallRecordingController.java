package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.*;
import com.qiaben.ciyex.service.telnyx.TeXmlCallRecordingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/texml/Accounts/{accountSid}/Calls/{callSid}/Recordings")
@RequiredArgsConstructor
public class TeXmlCallRecordingController {

    private final TeXmlCallRecordingService recordingService;

    // 1. Start a recording on a call
    @PostMapping
    public ResponseEntity<TeXmlCallRecordingResponseDto> startRecording(
            @PathVariable String accountSid,
            @PathVariable String callSid,
            @ModelAttribute TeXmlCallRecordingRequestDto body) {
        TeXmlCallRecordingResponseDto response = recordingService.startCallRecording(accountSid, callSid, body);
        return ResponseEntity.ok(response);
    }

    // 2. List recordings for a call
    @GetMapping
    public ResponseEntity<TeXmlCallRecordingListResponseDto> listRecordings(
            @PathVariable String accountSid,
            @PathVariable String callSid) {
        TeXmlCallRecordingListResponseDto response = recordingService.getCallRecordings(accountSid, callSid);
        return ResponseEntity.ok(response);
    }

    // 3. Update a specific recording (pause / stop / resume)
    @PostMapping("/{recordingSid}.json")
    public ResponseEntity<TeXmlCallRecordingResponseDto> updateRecording(
            @PathVariable String accountSid,
            @PathVariable String callSid,
            @PathVariable String recordingSid,
            @ModelAttribute TeXmlUpdateCallRecordingRequestDto requestDto) {
        TeXmlCallRecordingResponseDto response = recordingService.updateCallRecording(
                accountSid, callSid, recordingSid, requestDto);
        return ResponseEntity.ok(response);
    }
}
