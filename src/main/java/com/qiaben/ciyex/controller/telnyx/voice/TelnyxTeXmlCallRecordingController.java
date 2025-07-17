package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTeXmlCallRecordingListResponseDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTeXmlCallRecordingRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTeXmlCallRecordingResponseDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTeXmlUpdateCallRecordingRequestDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxTeXmlCallRecordingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/texml/Accounts/{accountSid}/Calls/{callSid}/Recordings")
@RequiredArgsConstructor
public class TelnyxTeXmlCallRecordingController {

    private final TelnyxTeXmlCallRecordingService recordingService;

    // 1. Start a recording on a call
    @PostMapping
    public ResponseEntity<TelnyxTeXmlCallRecordingResponseDto> startRecording(
            @PathVariable String accountSid,
            @PathVariable String callSid,
            @ModelAttribute TelnyxTeXmlCallRecordingRequestDto body) {
        TelnyxTeXmlCallRecordingResponseDto response = recordingService.startCallRecording(accountSid, callSid, body);
        return ResponseEntity.ok(response);
    }

    // 2. List recordings for a call
    @GetMapping
    public ResponseEntity<TelnyxTeXmlCallRecordingListResponseDto> listRecordings(
            @PathVariable String accountSid,
            @PathVariable String callSid) {
        TelnyxTeXmlCallRecordingListResponseDto response = recordingService.getCallRecordings(accountSid, callSid);
        return ResponseEntity.ok(response);
    }

    // 3. Update a specific recording (pause / stop / resume)
    @PostMapping("/{recordingSid}.json")
    public ResponseEntity<TelnyxTeXmlCallRecordingResponseDto> updateRecording(
            @PathVariable String accountSid,
            @PathVariable String callSid,
            @PathVariable String recordingSid,
            @ModelAttribute TelnyxTeXmlUpdateCallRecordingRequestDto requestDto) {
        TelnyxTeXmlCallRecordingResponseDto response = recordingService.updateCallRecording(
                accountSid, callSid, recordingSid, requestDto);
        return ResponseEntity.ok(response);
    }
}
